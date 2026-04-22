package org.vnu.sme.goal.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.JTextArea;

import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.util.Selection;
import org.tzi.use.gui.util.PersistHelper;
import org.tzi.use.gui.views.View;
import org.tzi.use.gui.views.diagrams.DiagramOptions;
import org.tzi.use.gui.views.diagrams.DiagramView;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.tzi.use.gui.views.diagrams.elements.edges.EdgeBase;
import org.tzi.use.uml.mm.MModel;
import org.vnu.sme.goal.mm.Actor;
import org.vnu.sme.goal.mm.Agent;
import org.vnu.sme.goal.mm.ContributionRelation;
import org.vnu.sme.goal.mm.Dependency;
import org.vnu.sme.goal.mm.Goal;
import org.vnu.sme.goal.mm.GoalModel;
import org.vnu.sme.goal.mm.IntentionalElement;
import org.vnu.sme.goal.mm.QualificationRelation;
import org.vnu.sme.goal.mm.Quality;
import org.vnu.sme.goal.mm.RefinementRelation;
import org.vnu.sme.goal.mm.Relation;
import org.vnu.sme.goal.mm.Resource;
import org.vnu.sme.goal.mm.Role;
import org.vnu.sme.goal.mm.Task;
import org.vnu.sme.goal.view.edges.ContributionEdge;
import org.vnu.sme.goal.view.edges.DependencyEdge;
import org.vnu.sme.goal.view.edges.QualificationEdge;
import org.vnu.sme.goal.view.edges.RefinementEdge;
import org.vnu.sme.goal.view.nodes.ActorBoundaryNode;
import org.vnu.sme.goal.view.nodes.ActorNode;
import org.vnu.sme.goal.view.nodes.AgentNode;
import org.vnu.sme.goal.view.nodes.GoalNode;
import org.vnu.sme.goal.view.nodes.QualityNode;
import org.vnu.sme.goal.view.nodes.ResourceNode;
import org.vnu.sme.goal.view.nodes.RoleNode;
import org.vnu.sme.goal.view.nodes.TaskNode;
import org.w3c.dom.Element;

public class GoalDiagramView extends DiagramView implements View {

    private final GoalModel goalModel;
    private final GoalDiagramData diagramData = new GoalDiagramData();
    private final GoalDiagramData hiddenData = new GoalDiagramData();
    private final Map<Actor, ActorNode> actorNodeMap = new HashMap<>();
    private final Map<Actor, ActorBoundaryNode> actorBoundaryMap = new HashMap<>();
    private final Map<PlaceableNode, Actor> nodeOwnerMap = new HashMap<>();
    private final Map<IntentionalElement, PlaceableNode> elementNodeMap = new HashMap<>();
    private final Map<IntentionalElement, OclValidationReport> validationReports = new HashMap<>();
    private final DiagramOptions diagramOptions;
    private final OclUseValidator oclValidator;
    private final GoalDiagramInputHandling inputHandling;

    public GoalDiagramView(MainWindow mainWindow, GoalModel goalModel) {
        this(mainWindow, goalModel, null);
    }

    public GoalDiagramView(MainWindow mainWindow, GoalModel goalModel, MModel useModel) {
        super(new GoalDiagramOptions(), mainWindow.logWriter());
        this.goalModel = goalModel;
        this.diagramOptions = getOptions();
        this.oclValidator = new OclUseValidator(useModel);
        this.inputHandling = new GoalDiagramInputHandling(fNodeSelection, fEdgeSelection, this);

        initializeView();
        buildDiagram();
        setupKeyBindings();
        addMouseListener(inputHandling);
        addKeyListener(inputHandling);
        setFocusable(true);
    }

    private void initializeView() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setPreferredSize(new Dimension(1200, 800));
    }

    private void buildDiagram() {
        createActorNodes();
        createElementNodes();
        createDependencyDependumNodes();
        createRelationEdges();
        createDependencyEdges();
        validateOclExpressions();
        autoLayout();
        invalidateContent(true);
    }

    private void createActorNodes() {
        int x = 50;
        int y = 50;
        int xOffset = 310;
        int yOffset = 300;

        for (Actor actor : goalModel.getActors()) {
            ActorBoundaryNode boundaryNode = new ActorBoundaryNode(actor, diagramOptions);
            boundaryNode.setPosition(x, y);
            diagramData.addNode(boundaryNode);
            fGraph.add(boundaryNode);
            actorBoundaryMap.put(actor, boundaryNode);

            ActorNode node = createActorNode(actor);
            node.setPosition(x + 16, y + 28);
            diagramData.addNode(node);
            fGraph.add(node);
            actorNodeMap.put(actor, node);
            nodeOwnerMap.put(node, actor);
            boundaryNode.addOwnedNode(node);

            x += xOffset;
            if (x > 1050) {
                x = 50;
                y += yOffset;
            }
        }
    }

    private ActorNode createActorNode(Actor actor) {
        if (actor instanceof Agent) {
            return new AgentNode((Agent) actor, diagramOptions);
        }
        if (actor instanceof Role) {
            return new RoleNode((Role) actor, diagramOptions);
        }
        return new ActorNode(actor, diagramOptions);
    }

    private void createElementNodes() {
        for (Actor actor : goalModel.getActors()) {
            ActorNode actorNode = actorNodeMap.get(actor);
            if (actorNode == null) {
                continue;
            }

            int elementX = (int) actorNode.getX() + 150;
            int elementY = (int) actorNode.getY() + 12;
            for (IntentionalElement element : actor.getElements()) {
                if (isDependencyDependum(element)) {
                    continue;
                }

                PlaceableNode node = createElementNode(element);
                if (node == null) {
                    continue;
                }

                node.setPosition(elementX, elementY);
                diagramData.addNode(node);
                fGraph.add(node);
                elementNodeMap.put(element, node);
                nodeOwnerMap.put(node, actor);
                actorBoundaryMap.get(actor).addOwnedNode(node);
                elementY += 85;
            }
        }

        updateAllActorBoundaries();
    }

    private void createDependencyDependumNodes() {
        int x = 70;
        int y = dependencyStartY();
        int xOffset = 230;

        for (Dependency dependency : goalModel.getDependencies()) {
            IntentionalElement dependum = dependency.getDependum();
            if (elementNodeMap.containsKey(dependum)) {
                continue;
            }

            PlaceableNode node = createElementNode(dependum);
            if (node == null) {
                continue;
            }

            node.setPosition(x, y);
            diagramData.addNode(node);
            fGraph.add(node);
            elementNodeMap.put(dependum, node);

            x += xOffset;
            if (x > 1050) {
                x = 70;
                y += 100;
            }
        }
    }

    private PlaceableNode createElementNode(IntentionalElement element) {
        if (element instanceof Goal) {
            return new GoalNode((Goal) element, diagramOptions);
        }
        if (element instanceof Task) {
            return new TaskNode((Task) element, diagramOptions);
        }
        if (element instanceof Quality) {
            return new QualityNode((Quality) element, diagramOptions);
        }
        if (element instanceof Resource) {
            return new ResourceNode((Resource) element, diagramOptions);
        }
        return null;
    }

    private void createRelationEdges() {
        for (IntentionalElement element : goalModel.getAllElements()) {
            PlaceableNode sourceNode = elementNodeMap.get(element);
            if (sourceNode == null) {
                continue;
            }

            for (Relation relation : element.getOutgoingRelations()) {
                PlaceableNode targetNode = elementNodeMap.get(relation.getTarget());
                if (targetNode == null) {
                    continue;
                }

                EdgeBase edge = createRelationEdge(relation, sourceNode, targetNode);
                if (edge != null) {
                    diagramData.addEdge(edge);
                    fGraph.addEdge(edge);
                }
            }
        }
    }

    private EdgeBase createRelationEdge(Relation relation, PlaceableNode source, PlaceableNode target) {
        if (relation instanceof RefinementRelation) {
            return new RefinementEdge(source, target, (RefinementRelation) relation, this);
        }
        if (relation instanceof ContributionRelation) {
            return new ContributionEdge(source, target, (ContributionRelation) relation, this);
        }
        if (relation instanceof QualificationRelation) {
            return new QualificationEdge(source, target, (QualificationRelation) relation, this);
        }
        return null;
    }

    private void createDependencyEdges() {
        for (Dependency dependency : goalModel.getDependencies()) {
            ActorNode dependerNode = actorNodeMap.get(dependency.getDepender());
            ActorNode dependeeNode = actorNodeMap.get(dependency.getDependee());
            PlaceableNode dependumNode = elementNodeMap.get(dependency.getDependum());

            if (dependerNode != null && dependeeNode != null && dependumNode != null) {
                DependencyEdge edge = new DependencyEdge(dependerNode, dependeeNode, dependumNode, dependency, this);
                diagramData.addEdge(edge);
                fGraph.addEdge(edge);
            }
        }
    }

    public Selection<PlaceableNode> getNodeSelection() {
        return fNodeSelection;
    }

    public void moveActorGroup(Actor actor, double dx, double dy) {
        ActorNode actorNode = actorNodeMap.get(actor);
        if (actorNode != null) {
            actorNode.setDraggedPosition(dx, dy);
            invalidateNode(actorNode);
        }

        for (IntentionalElement element : actor.getElements()) {
            if (isDependencyDependum(element)) {
                continue;
            }

            PlaceableNode elementNode = elementNodeMap.get(element);
            if (elementNode != null) {
                elementNode.setDraggedPosition(dx, dy);
                invalidateNode(elementNode);
            }
        }

        ActorBoundaryNode boundaryNode = actorBoundaryMap.get(actor);
        if (boundaryNode != null) {
            boundaryNode.setDraggedPosition(dx, dy);
            boundaryNode.updateFromOwnedNodes();
            invalidateNode(boundaryNode);
        }
    }

    public void updateBoundaryForElement(PlaceableNode node) {
        Actor actor = nodeOwnerMap.get(node);
        if (actor == null && node instanceof ActorBoundaryNode) {
            actor = ((ActorBoundaryNode) node).getActor();
        }
        if (actor == null) {
            return;
        }

        ActorBoundaryNode boundaryNode = actorBoundaryMap.get(actor);
        if (boundaryNode != null) {
            boundaryNode.updateFromOwnedNodes();
            invalidateNode(boundaryNode);
        }
    }

    private void updateAllActorBoundaries() {
        for (ActorBoundaryNode boundaryNode : actorBoundaryMap.values()) {
            boundaryNode.updateFromOwnedNodes();
            invalidateNode(boundaryNode);
        }
    }

    @Override
    protected void autoLayout() {
        // Initial layout is deterministic; USE can still persist manual placements.
    }

    private void setupKeyBindings() {
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F5"), "refresh");
        getActionMap().put("refresh", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshDiagram();
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F6"), "validateOcl");
        getActionMap().put("validateOcl", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOclValidationReport();
            }
        });
    }

    public void refreshDiagram() {
        diagramData.clear();
        hiddenData.clear();
        fGraph.clear();
        actorNodeMap.clear();
        actorBoundaryMap.clear();
        nodeOwnerMap.clear();
        elementNodeMap.clear();
        validationReports.clear();
        buildDiagram();
    }

    @Override
    public void paintComponent(Graphics g) {
        updateAllActorBoundaries();
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.BLACK);
        g2d.drawString("Goal Model: " + goalModel.getName(), 10, 20);
    }

    @Override
    public Set<? extends PlaceableNode> getHiddenNodes() {
        return hiddenData.getNodes();
    }

    @Override
    public DiagramData getVisibleData() {
        return diagramData;
    }

    @Override
    public DiagramData getHiddenData() {
        return hiddenData;
    }

    @Override
    public void storePlacementInfos(PersistHelper helper, Element rootElement) {
        for (PlaceableNode node : diagramData.getNodes()) {
            node.storePlacementInfo(helper, rootElement, false);
        }
        for (EdgeBase edge : diagramData.getEdges()) {
            edge.storePlacementInfo(helper, rootElement, false);
        }
    }

    @Override
    public void restorePlacementInfos(PersistHelper helper, int version) {
        // GOAL layout restore is not versioned yet.
    }

    @Override
    public void showAll() {
        if (!diagramData.hasNodes() && hiddenData.hasNodes()) {
            for (PlaceableNode node : new HashSet<>(hiddenData.getNodes())) {
                diagramData.addNode(node);
                fGraph.add(node);
            }
            for (EdgeBase edge : new HashSet<>(hiddenData.getEdges())) {
                diagramData.addEdge(edge);
                fGraph.addEdge(edge);
            }
            hiddenData.clear();
            repaint();
        }
    }

    @Override
    public void hideAll() {
        for (EdgeBase edge : new HashSet<>(diagramData.getEdges())) {
            hiddenData.addEdge(edge);
            fGraph.removeEdge(edge);
        }
        for (PlaceableNode node : new HashSet<>(diagramData.getNodes())) {
            hiddenData.addNode(node);
            fGraph.remove(node);
        }
        diagramData.clear();
        repaint();
    }

    @Override
    protected String getDefaultLayoutFileSuffix() {
        return "_goal_default.clt";
    }

    @Override
    public void detachModel() {
        stopLayoutThread();
    }

    private boolean isDependencyDependum(IntentionalElement element) {
        for (Dependency dependency : goalModel.getDependencies()) {
            if (dependency.getDependum() == element) {
                return true;
            }
        }
        return false;
    }

    private int dependencyStartY() {
        double maxY = 0;
        for (ActorBoundaryNode boundaryNode : actorBoundaryMap.values()) {
            maxY = Math.max(maxY, boundaryNode.getBounds().getMaxY());
        }
        return (int) maxY + 70;
    }

    private void validateOclExpressions() {
        validationReports.clear();
        for (IntentionalElement element : goalModel.getAllElements()) {
            if (element instanceof Goal || element instanceof Task) {
                validationReports.put(element, oclValidator.validate(element));
            }
        }
    }

    private void showOclValidationReport() {
        validateOclExpressions();
        StringBuilder reportText = new StringBuilder();
        reportText.append("OCL validation against USE model\n");
        reportText.append("Goal model: ").append(goalModel.getName()).append("\n\n");

        int checked = 0;
        int problems = 0;
        for (Map.Entry<IntentionalElement, OclValidationReport> entry : validationReports.entrySet()) {
            IntentionalElement element = entry.getKey();
            OclValidationReport report = entry.getValue();
            checked++;

            reportText.append(element.getType()).append(" ").append(element.getName()).append(": ");
            if (!report.hasProblems()) {
                reportText.append("OK\n");
                continue;
            }

            problems++;
            reportText.append("Mismatch\n");
            for (String message : report.getMessages()) {
                reportText.append("  - ").append(message).append("\n");
            }
        }

        if (checked == 0) {
            reportText.append("No goal/task OCL expressions found.\n");
        } else {
            reportText.append("\nChecked: ").append(checked)
                    .append(", mismatched: ").append(problems).append("\n");
        }

        JTextArea area = new JTextArea(reportText.toString(), 22, 72);
        area.setEditable(false);
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "GOAL OCL validation", JOptionPane.INFORMATION_MESSAGE);
    }
}
