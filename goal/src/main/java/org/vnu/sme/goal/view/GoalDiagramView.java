package org.vnu.sme.goal.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.util.PersistHelper;
import org.tzi.use.gui.util.Selection;
import org.tzi.use.gui.views.View;
import org.tzi.use.gui.views.diagrams.DiagramOptions;
import org.tzi.use.gui.views.diagrams.DiagramView;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.tzi.use.gui.views.diagrams.elements.edges.EdgeBase;
import org.tzi.use.gui.views.diagrams.event.ActionLoadLayout;
import org.tzi.use.gui.views.diagrams.event.ActionSaveLayout;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.ocl.value.VarBindings;
import org.tzi.use.uml.sys.MSystem;
import org.tzi.use.uml.sys.MSystemState;
import org.vnu.sme.goal.mm.bpmn.BpmnModel;
import org.vnu.sme.goal.mm.Actor;
import org.vnu.sme.goal.mm.Agent;
import org.vnu.sme.goal.mm.ConcreteIntentionalElement;
import org.vnu.sme.goal.mm.Contribution;
import org.vnu.sme.goal.mm.Dependency;
import org.vnu.sme.goal.mm.Goal;
import org.vnu.sme.goal.mm.GoalModel;
import org.vnu.sme.goal.mm.IntentionalElement;
import org.vnu.sme.goal.mm.Quality;
import org.vnu.sme.goal.mm.Refinement;
import org.vnu.sme.goal.mm.Resource;
import org.vnu.sme.goal.mm.Role;
import org.vnu.sme.goal.mm.Task;
import org.vnu.sme.goal.parser.GoalOclService;
import org.vnu.sme.goal.validator.GoalBpmnValidator;
import org.vnu.sme.goal.view.edges.ContributionEdge;
import org.vnu.sme.goal.view.edges.DependencyEdge;
import org.vnu.sme.goal.view.edges.NeededByEdge;
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
    private enum DisplayMode {
        ALL,
        ACTORS_ONLY,
        DEPENDENCY_OVERVIEW,
        ACTOR_FOCUS
    }

    private final GoalModel goalModel;
    private final GoalDiagramData diagramData = new GoalDiagramData();
    private final GoalDiagramData hiddenData = new GoalDiagramData();
    private final Map<Actor, ActorNode> actorNodeMap = new HashMap<>();
    private final Map<Actor, ActorBoundaryNode> actorBoundaryMap = new HashMap<>();
    private final Map<PlaceableNode, Actor> nodeOwnerMap = new HashMap<>();
    private final Map<IntentionalElement, PlaceableNode> elementNodeMap = new HashMap<>();
    private final Map<String, GoalBpmnValidator.GoalCheckRow> latestBpmnGoalRows = new LinkedHashMap<>();
    private final DiagramOptions diagramOptions;
    private final GoalOclService goalOclService;
    private final GoalDiagramInputHandling inputHandling;

    private DisplayMode displayMode = DisplayMode.ALL;
    private Actor focusedActor;

    public GoalDiagramView(MainWindow mainWindow, GoalModel goalModel) {
        this(mainWindow, goalModel, (MSystem) null);
    }

    public GoalDiagramView(MainWindow mainWindow, GoalModel goalModel, MSystem system) {
        this(mainWindow, goalModel, system == null ? null : system.model(),
                system == null ? null : system.state(),
                system == null ? null : system.varBindings());
    }

    public GoalDiagramView(MainWindow mainWindow, GoalModel goalModel, MModel useModel) {
        this(mainWindow, goalModel, useModel, null, null);
    }

    public GoalDiagramView(MainWindow mainWindow, GoalModel goalModel, MModel useModel,
                           MSystemState systemState, VarBindings varBindings) {
        super(new GoalDiagramOptions(), mainWindow.logWriter());
        this.goalModel = goalModel;
        this.diagramOptions = getOptions();
        this.goalOclService = new GoalOclService(useModel, systemState, varBindings);
        this.inputHandling = new GoalDiagramInputHandling(fNodeSelection, fEdgeSelection, this);
        this.fActionSaveLayout = new ActionSaveLayout("USE goal diagram layout", "glt", this);
        this.fActionLoadLayout = new ActionLoadLayout("USE goal diagram layout", "glt", this);

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
        autoLayout();
        invalidateContent(true);
    }

    private void createActorNodes() {
        int x = 50;
        int y = 50;
        int xOffset = 310;
        int yOffset = 300;

        for (Actor actor : goalModel.getActors()) {
            if (!shouldShowActor(actor)) {
                continue;
            }

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
            if (!shouldShowActor(actor)) {
                continue;
            }

            ActorNode actorNode = actorNodeMap.get(actor);
            if (actorNode == null) {
                continue;
            }

            int elementX = (int) actorNode.getX() + 150;
            int elementY = (int) actorNode.getY() + 12;
            for (IntentionalElement element : actor.getElements()) {
                if (!shouldShowOwnedElement(actor, element) || isDependencyDependum(element)) {
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
            if (!shouldShowDependency(dependency)) {
                continue;
            }

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
            GoalNode node = new GoalNode((Goal) element, diagramOptions);
            applyStoredBpmnVerification((Goal) element, node);
            return node;
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
        if (displayMode != DisplayMode.ACTORS_ONLY) {
            for (Contribution contribution : goalModel.getContributions()) {
                PlaceableNode sourceNode = elementNodeMap.get(contribution.getSource());
                PlaceableNode targetNode = elementNodeMap.get(contribution.getTarget());
                if (sourceNode != null && targetNode != null) {
                    EdgeBase edge = new ContributionEdge(sourceNode, targetNode, contribution, this);
                    diagramData.addEdge(edge);
                    fGraph.addEdge(edge);
                }
            }

            for (Refinement refinement : goalModel.getRefinements()) {
                PlaceableNode targetNode = elementNodeMap.get(refinement.getParent());
                if (targetNode == null) {
                    continue;
                }
                for (org.vnu.sme.goal.mm.GoalTaskElement child : refinement.getChildren()) {
                    PlaceableNode sourceNode = elementNodeMap.get(child);
                    if (sourceNode != null) {
                        EdgeBase edge = new RefinementEdge(sourceNode, targetNode, refinement, this);
                        diagramData.addEdge(edge);
                        fGraph.addEdge(edge);
                    }
                }
            }

            for (IntentionalElement element : goalModel.getAllElements()) {
                if (element instanceof Quality quality) {
                    PlaceableNode sourceNode = elementNodeMap.get(quality);
                    if (sourceNode == null) {
                        continue;
                    }
                    for (ConcreteIntentionalElement qualified : quality.getQualifiedElements()) {
                        PlaceableNode targetNode = elementNodeMap.get(qualified);
                        if (targetNode != null) {
                            EdgeBase edge = new QualificationEdge(
                                    sourceNode,
                                    targetNode,
                                    quality.getName() + "_qualifies_" + qualified.getName(),
                                    this);
                            diagramData.addEdge(edge);
                            fGraph.addEdge(edge);
                        }
                    }
                } else if (element instanceof Resource resource) {
                    PlaceableNode sourceNode = elementNodeMap.get(resource);
                    if (sourceNode == null) {
                        continue;
                    }
                    for (Task task : resource.getNeededByTasks()) {
                        PlaceableNode targetNode = elementNodeMap.get(task);
                        if (targetNode != null) {
                            EdgeBase edge = new NeededByEdge(
                                    sourceNode,
                                    targetNode,
                                    resource.getName() + "_neededBy_" + task.getName(),
                                    this);
                            diagramData.addEdge(edge);
                            fGraph.addEdge(edge);
                        }
                    }
                }
            }
        }
    }

    private void createDependencyEdges() {
        if (displayMode == DisplayMode.ACTORS_ONLY) {
            return;
        }

        for (Dependency dependency : goalModel.getDependencies()) {
            if (!shouldShowDependency(dependency)) {
                continue;
            }

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
        if (actor == null && node instanceof ActorBoundaryNode boundaryNode) {
            actor = boundaryNode.getActor();
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

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F7"), "goalStatus");
        getActionMap().put("goalStatus", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showGoalStatusTable();
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F8"), "goalDesign");
        getActionMap().put("goalDesign", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showGoalDesignReport();
            }
        });

        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("F9"), "goalBpmn");
        getActionMap().put("goalBpmn", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                BpmnModel bpmnModel = GoalViewRegistry.getCurrentBpmnModel();
                if (bpmnModel == null) {
                    JOptionPane.showMessageDialog(
                            GoalDiagramView.this,
                            "No BPMN model is loaded yet. Load a .bpmn file first.",
                            "GOAL + BPMN verification",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                showBpmnVerificationReport(bpmnModel, GoalViewRegistry.getCurrentBpmnSource());
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
        buildDiagram();
    }

    public void showActorsOnly() {
        displayMode = DisplayMode.ACTORS_ONLY;
        focusedActor = null;
        refreshDiagram();
    }

    public void showDependencyOverview() {
        displayMode = DisplayMode.DEPENDENCY_OVERVIEW;
        focusedActor = null;
        refreshDiagram();
    }

    public void showActorContext(Actor actor) {
        if (actor == null) {
            return;
        }
        displayMode = DisplayMode.ACTOR_FOCUS;
        focusedActor = actor;
        refreshDiagram();
    }

    public void showFullModel() {
        displayMode = DisplayMode.ALL;
        focusedActor = null;
        refreshDiagram();
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
        showFullModel();
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

    @Override
    protected PopupMenuInfo unionOfPopUpMenu() {
        PopupMenuInfo info = super.unionOfPopUpMenu();
        info.popupMenu.addSeparator();
        info.popupMenu.add(new JMenuItem(new AbstractAction("Show full GOAL model") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showFullModel();
            }
        }));
        info.popupMenu.add(new JMenuItem(new AbstractAction("Show actors only") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showActorsOnly();
            }
        }));
        info.popupMenu.add(new JMenuItem(new AbstractAction("Show actors + dependencies") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showDependencyOverview();
            }
        }));

        Actor selectedActor = getSelectedContextActor();
        if (selectedActor != null) {
            info.popupMenu.add(new JMenuItem(new AbstractAction("Focus selected actor: " + selectedActor.getName()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showActorContext(selectedActor);
                }
            }));
        }

        JMenu focusMenu = new JMenu("Focus actor");
        for (Actor actor : goalModel.getActors()) {
            focusMenu.add(new JMenuItem(new AbstractAction(actor.getName()) {
                @Override
                public void actionPerformed(ActionEvent e) {
                    showActorContext(actor);
                }
            }));
        }
        info.popupMenu.add(focusMenu);

        info.popupMenu.add(new JMenuItem(new AbstractAction("Refresh GOAL diagram") {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshDiagram();
            }
        }));
        info.popupMenu.add(new JMenuItem(new AbstractAction("Validate GOAL OCL") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showOclValidationReport();
            }
        }));
        info.popupMenu.add(new JMenuItem(new AbstractAction("Show GOAL status table") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showGoalStatusTable();
            }
        }));
        info.popupMenu.add(new JMenuItem(new AbstractAction("Analyze GOAL design") {
            @Override
            public void actionPerformed(ActionEvent e) {
                showGoalDesignReport();
            }
        }));
        info.popupMenu.add(new JMenuItem(new AbstractAction("Verify GOAL with BPMN") {
            @Override
            public void actionPerformed(ActionEvent e) {
                BpmnModel bpmnModel = GoalViewRegistry.getCurrentBpmnModel();
                if (bpmnModel == null) {
                    JOptionPane.showMessageDialog(
                            GoalDiagramView.this,
                            "No BPMN model is loaded yet. Load a .bpmn file first.",
                            "GOAL + BPMN verification",
                            JOptionPane.INFORMATION_MESSAGE);
                    return;
                }
                showBpmnVerificationReport(bpmnModel, GoalViewRegistry.getCurrentBpmnSource());
            }
        }));
        return info;
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

    private void showOclValidationReport() {
        StringBuilder reportText = new StringBuilder();
        reportText.append("OCL syntax/type validation using USE OCL compiler\n");
        reportText.append("Goal model: ").append(goalModel.getName()).append("\n\n");

        int checked = 0;
        int problems = 0;

        for (IntentionalElement element : goalModel.getAllElements()) {
            if (element instanceof Goal goal) {
                checked += appendValidation(reportText, element, "goal", goal.getOclExpression());
                if (hasValidationProblem(goal.getOclExpression(), element, "goal")) {
                    problems++;
                }
            } else if (element instanceof Task task) {
                checked += appendValidation(reportText, element, "pre", task.getPreExpression());
                checked += appendValidation(reportText, element, "post", task.getPostExpression());
                if (hasValidationProblem(task.getPreExpression(), element, "pre")) {
                    problems++;
                }
                if (hasValidationProblem(task.getPostExpression(), element, "post")) {
                    problems++;
                }
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

    public void showGoalStatusTable() {
        DefaultTableModel model = new DefaultTableModel(
                new Object[] { "Actor", "Goal", "Type", "Syntax", "OCL value", "Goal status", "Detail" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Actor actor : goalModel.getActors()) {
            for (IntentionalElement element : actor.getElements()) {
                if (!(element instanceof Goal goal)) {
                    continue;
                }

                String goalType = goal.getGoalType() == null ? "none" : goal.getGoalType().name().toLowerCase();
                String expression = goal.getOclExpression();
                if (expression == null || expression.isBlank()) {
                    model.addRow(new Object[] {
                            actor.getName(),
                            goal.getName(),
                            goalType,
                            "NONE",
                            "SKIPPED",
                            "INCOMPLETE",
                            "Goal has no OCL clause yet."
                    });
                    continue;
                }

                GoalOclService.CompilationResult syntax =
                        goalOclService.validateExpression(expression, "goal:" + goal.getName());
                GoalOclService.EvalResult eval =
                        goalOclService.evaluateBooleanExpression(expression, "goal:" + goal.getName());

                model.addRow(new Object[] {
                        actor.getName(),
                        goal.getName(),
                        goalType,
                        syntax.ok() ? "OK" : "ERROR",
                        eval.kind(),
                        deriveGoalStatus(goal, eval),
                        eval.detail()
                });
            }
        }

        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(6).setPreferredWidth(520);

        JTextArea explanation = new JTextArea(goalStatusLegend(), 8, 80);
        explanation.setEditable(false);
        explanation.setCaretPosition(0);
        explanation.setLineWrap(true);
        explanation.setWrapStyleWord(true);

        JDialog dialog = new JDialog(MainWindow.instance(), "GOAL status table", false);
        dialog.setLayout(new BorderLayout(8, 8));
        dialog.add(new JScrollPane(table), BorderLayout.CENTER);
        dialog.add(new JScrollPane(explanation), BorderLayout.SOUTH);
        dialog.setSize(980, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private String goalStatusLegend() {
        return String.join("\n",
                "Validator OCL = compile-time syntax/type check using USE OCL compiler.",
                "Status table = runtime evaluation on the current USE SystemState.",
                "Goals without OCL are treated as incomplete design elements and are not evaluated.",
                "OCL value is the raw boolean result of the goal expression: TRUE / FALSE / UNDEFINED / ERROR.",
                "Goal status is derived from goal kind:",
                "- achieve, maintain: TRUE => satisfied, FALSE => violated",
                "- avoid: TRUE => violated, FALSE => satisfied");
    }

    public void showGoalDesignReport() {
        GoalDesignAnalyzer analyzer = new GoalDesignAnalyzer(goalModel, goalOclService);
        List<GoalDesignAnalyzer.DesignIssue> issues = analyzer.analyze();

        StringBuilder report = new StringBuilder();
        report.append("GOAL design analysis\n");
        report.append("Goal model: ").append(goalModel.getName()).append("\n\n");
        report.append("This report answers whether the model is operationalized well enough to reach goals,\n");
        report.append("separately from whether the OCL currently evaluates to true or false.\n\n");

        if (issues.isEmpty()) {
            report.append("No design issues found.\n");
        } else {
            for (GoalDesignAnalyzer.DesignIssue issue : issues) {
                report.append("[").append(issue.severity()).append("] ")
                        .append(issue.location()).append(": ")
                        .append(issue.message()).append("\n");
            }
        }

        JTextArea area = new JTextArea(report.toString(), 24, 88);
        area.setEditable(false);
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "GOAL design analysis", JOptionPane.INFORMATION_MESSAGE);
    }

    public void showBpmnVerificationReport(BpmnModel bpmnModel, String sourceName) {
        GoalBpmnValidator validator = new GoalBpmnValidator(goalModel, bpmnModel, goalOclService);
        GoalBpmnValidator.AnalysisReport report = validator.analyze();
        applyBpmnVerification(report);

        DefaultTableModel tableModel = new DefaultTableModel(
                new Object[] { "Actor", "Goal", "Type", "Goal state", "Detail" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (GoalBpmnValidator.GoalCheckRow row : report.goalRows()) {
            tableModel.addRow(new Object[] {
                    row.actor(),
                    row.goal(),
                    row.type(),
                    row.status(),
                    row.detail()
            });
        }

        JTable table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getColumnModel().getColumn(4).setPreferredWidth(520);
        table.getColumnModel().getColumn(3).setCellRenderer(new VerificationStatusRenderer());
        table.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) {
                return;
            }
            int modelRow = table.convertRowIndexToModel(viewRow);
            Object actorName = tableModel.getValueAt(modelRow, 0);
            Object goalName = tableModel.getValueAt(modelRow, 1);
            focusVerificationContext(
                    actorName == null ? null : actorName.toString(),
                    goalName == null ? null : goalName.toString(),
                    null,
                    goalName == null ? null : goalName.toString());
        });

        DefaultTableModel obligationModel = new DefaultTableModel(
                new Object[] { "Actor", "Goal", "Kind", "From", "To", "Status", "Expression" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        for (GoalBpmnValidator.ObligationRow row : report.obligationRows()) {
            obligationModel.addRow(new Object[] {
                    row.actor(),
                    row.goal(),
                    row.obligationType(),
                    row.sourceTask(),
                    row.target(),
                    row.status(),
                    row.expression()
            });
        }

        JTable obligationTable = new JTable(obligationModel);
        obligationTable.setAutoCreateRowSorter(true);
        obligationTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        obligationTable.getColumnModel().getColumn(5).setCellRenderer(new VerificationStatusRenderer());
        obligationTable.getColumnModel().getColumn(6).setPreferredWidth(460);
        obligationTable.getSelectionModel().addListSelectionListener(event -> {
            if (event.getValueIsAdjusting()) {
                return;
            }
            int viewRow = obligationTable.getSelectedRow();
            if (viewRow < 0) {
                return;
            }
            int modelRow = obligationTable.convertRowIndexToModel(viewRow);
            Object actorName = obligationModel.getValueAt(modelRow, 0);
            Object goalName = obligationModel.getValueAt(modelRow, 1);
            Object sourceTaskName = obligationModel.getValueAt(modelRow, 3);
            Object targetElementName = obligationModel.getValueAt(modelRow, 4);
            focusVerificationContext(
                    actorName == null ? null : actorName.toString(),
                    goalName == null ? null : goalName.toString(),
                    sourceTaskName == null ? null : sourceTaskName.toString(),
                    targetElementName == null ? null : targetElementName.toString());
        });

        JTextArea area = new JTextArea(validator.renderReport(sourceName), 18, 92);
        area.setEditable(false);
        area.setCaretPosition(0);

        JDialog dialog = new JDialog(MainWindow.instance(), "GOAL + BPMN verification", false);
        dialog.setLayout(new BorderLayout(8, 8));
        JTextArea legend = new JTextArea(
                "Top table: final goal proof results.\n"
                        + "Middle table: generated proof obligations such as post(T1) => pre(T2) and post(Tn) => goal.\n"
                        + "Bottom report: full textual explanation of the proof pipeline.\n"
                        + "This validator uses symbolic OCL entailment on the GOAL OCL metamodel, not runtime state evaluation.",
                3, 92);
        legend.setEditable(false);
        legend.setLineWrap(true);
        legend.setWrapStyleWord(true);

        JPanel centerPanel = new JPanel(new BorderLayout(8, 8));
        centerPanel.add(new JScrollPane(table), BorderLayout.NORTH);
        centerPanel.add(new JScrollPane(obligationTable), BorderLayout.CENTER);

        dialog.add(centerPanel, BorderLayout.CENTER);
        dialog.add(new JScrollPane(legend), BorderLayout.NORTH);
        dialog.add(new JScrollPane(area), BorderLayout.SOUTH);
        dialog.setSize(1180, 760);
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private int appendValidation(StringBuilder reportText, IntentionalElement element, String clauseName, String expression) {
        if (expression == null || expression.isBlank()) {
            return 0;
        }

        GoalOclService.CompilationResult result =
                goalOclService.validateExpression(expression, element.getType() + ":" + element.getName() + ":" + clauseName);
        reportText.append(element.getType()).append(" ").append(element.getName())
                .append(" [").append(clauseName).append("]: ");
        if (result.ok()) {
            reportText.append("OK\n");
        } else {
            reportText.append("ERROR\n");
            reportText.append("  - original: ").append(expression).append("\n");
            if (result.normalizedExpression() != null && !result.normalizedExpression().equals(expression)) {
                reportText.append("  - normalized: ").append(result.normalizedExpression()).append("\n");
            }
            for (String line : result.detail().split("\\R")) {
                if (!line.isBlank()) {
                    reportText.append("  - ").append(line).append("\n");
                }
            }
        }
        return 1;
    }

    private boolean hasValidationProblem(String expression, IntentionalElement element, String clauseName) {
        if (expression == null || expression.isBlank()) {
            return false;
        }
        return !goalOclService
                .validateExpression(expression, element.getType() + ":" + element.getName() + ":" + clauseName)
                .ok();
    }

    private String deriveGoalStatus(Goal goal, GoalOclService.EvalResult eval) {
        if (goal.getGoalType() == null) {
            return "NONE";
        }
        if (eval.kind() == GoalOclService.EvalKind.ERROR || eval.kind() == GoalOclService.EvalKind.UNDEFINED) {
            return eval.kind().name();
        }

        boolean truth = eval.kind() == GoalOclService.EvalKind.TRUE;
        return switch (goal.getGoalType()) {
            case ACHIEVE, MAINTAIN -> truth ? "SATISFIED" : "VIOLATED";
            case AVOID -> truth ? "VIOLATED" : "SATISFIED";
        };
    }

    private boolean shouldShowActor(Actor actor) {
        return switch (displayMode) {
            case ALL, ACTORS_ONLY, DEPENDENCY_OVERVIEW -> true;
            case ACTOR_FOCUS -> actor == focusedActor;
        };
    }

    private boolean shouldShowOwnedElement(Actor actor, IntentionalElement element) {
        return switch (displayMode) {
            case ALL -> true;
            case ACTORS_ONLY -> false;
            case DEPENDENCY_OVERVIEW -> participatesInVisibleDependency(actor, element);
            case ACTOR_FOCUS -> actor == focusedActor;
        };
    }

    private boolean shouldShowDependency(Dependency dependency) {
        return switch (displayMode) {
            case ALL, DEPENDENCY_OVERVIEW -> true;
            case ACTORS_ONLY -> false;
            case ACTOR_FOCUS -> dependency.getDepender() == focusedActor || dependency.getDependee() == focusedActor;
        };
    }

    private boolean participatesInVisibleDependency(Actor actor, IntentionalElement element) {
        for (Dependency dependency : goalModel.getDependencies()) {
            if (dependency.getDepender() == actor || dependency.getDependee() == actor || dependency.getDependum() == element) {
                return true;
            }
        }
        return false;
    }

    private Actor getSelectedContextActor() {
        for (PlaceableNode node : fNodeSelection) {
            if (node instanceof ActorBoundaryNode boundaryNode) {
                return boundaryNode.getActor();
            }
            if (node instanceof ActorNode actorNode) {
                return actorNode.getActor();
            }
            Actor owner = nodeOwnerMap.get(node);
            if (owner != null) {
                return owner;
            }
        }
        return null;
    }

    public GoalModel getGoalModel() {
        return goalModel;
    }

    private void applyStoredBpmnVerification(Goal goal, GoalNode node) {
        GoalBpmnValidator.GoalCheckRow row = latestBpmnGoalRows.get(goal.getName());
        if (row != null) {
            node.setVerificationState(row.status(), row.detail());
        }
    }

    private void applyBpmnVerification(GoalBpmnValidator.AnalysisReport report) {
        latestBpmnGoalRows.clear();
        for (GoalBpmnValidator.GoalCheckRow row : report.goalRows()) {
            latestBpmnGoalRows.put(row.goal(), row);
        }

        for (Map.Entry<IntentionalElement, PlaceableNode> entry : elementNodeMap.entrySet()) {
            if (!(entry.getKey() instanceof Goal goal) || !(entry.getValue() instanceof GoalNode goalNode)) {
                continue;
            }
            GoalBpmnValidator.GoalCheckRow row = latestBpmnGoalRows.get(goal.getName());
            if (row == null) {
                goalNode.setVerificationState(null, null);
            } else {
                goalNode.setVerificationState(row.status(), row.detail());
            }
            invalidateNode(goalNode);
        }
        invalidateContent(true);
        repaint();
    }

    private void focusVerificationContext(String actorName, String goalName, String sourceName, String targetName) {
        fNodeSelection.clear();

        PlaceableNode goalNode = findElementNode(actorName, goalName);
        PlaceableNode sourceNode = findElementNode(actorName, sourceName);
        PlaceableNode targetNode = findElementNode(actorName, targetName);

        if (goalNode != null) {
            fNodeSelection.add(goalNode);
        }
        if (sourceNode != null && sourceNode != goalNode) {
            fNodeSelection.add(sourceNode);
        }
        if (targetNode != null && targetNode != goalNode && targetNode != sourceNode) {
            fNodeSelection.add(targetNode);
        }

        invalidateContent(true);
        repaint();
    }

    private PlaceableNode findElementNode(String actorName, String elementName) {
        if (elementName == null || elementName.isBlank()) {
            return null;
        }

        for (Map.Entry<IntentionalElement, PlaceableNode> entry : elementNodeMap.entrySet()) {
            IntentionalElement element = entry.getKey();
            if (!elementName.equals(element.getName())) {
                continue;
            }
            if (actorName == null || actorName.isBlank()) {
                return entry.getValue();
            }
            Actor owner = element.getOwner();
            if (owner != null && actorName.equals(owner.getName())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private static final class VerificationStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                                boolean hasFocus, int row, int column) {
            java.awt.Component component =
                    super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if (isSelected) {
                return component;
            }

            String text = value == null ? "" : value.toString();
            component.setForeground(Color.BLACK);
            component.setBackground(Color.WHITE);
            if ("TRUE".equalsIgnoreCase(text)) {
                component.setBackground(new Color(220, 245, 223));
                component.setForeground(new Color(27, 94, 32));
            } else if ("FALSE".equalsIgnoreCase(text)) {
                component.setBackground(new Color(255, 230, 230));
                component.setForeground(new Color(183, 28, 28));
            }
            return component;
        }
    }
}
