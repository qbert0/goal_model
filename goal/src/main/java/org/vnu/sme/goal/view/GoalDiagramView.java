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
import java.util.Map;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import org.tzi.use.gui.main.MainWindow;
import org.tzi.use.gui.util.PersistHelper;
import org.tzi.use.gui.util.Selection;
import org.tzi.use.gui.views.View;
import org.tzi.use.gui.views.diagrams.DiagramOptions;
import org.tzi.use.gui.views.diagrams.DiagramView;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.tzi.use.gui.views.diagrams.elements.edges.EdgeBase;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.ocl.value.VarBindings;
import org.tzi.use.uml.sys.MSystem;
import org.tzi.use.uml.sys.MSystemState;
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
    private final Map<IntentionalElement, OclValidationReport> validationReports = new HashMap<>();
    private final DiagramOptions diagramOptions;
    private final OclUseValidator oclValidator;
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
        this.oclValidator = new OclUseValidator(useModel);
        this.goalOclService = new GoalOclService(useModel, systemState, varBindings);
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

    private void validateOclExpressions() {
        validationReports.clear();
        for (IntentionalElement element : goalModel.getAllElements()) {
            if (element instanceof Goal || element instanceof Task) {
                validationReports.put(element, oclValidator.validate(element));
            }
        }
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

                GoalOclService.CompilationResult syntax =
                        goalOclService.validateExpression(goal.getOclExpression(), "goal:" + goal.getName());
                GoalOclService.EvalResult eval =
                        goalOclService.evaluateBooleanExpression(goal.getOclExpression(), "goal:" + goal.getName());

                model.addRow(new Object[] {
                        actor.getName(),
                        goal.getName(),
                        goal.getGoalType() == null ? "unspecified" : goal.getGoalType().name().toLowerCase(),
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
            return "UNKNOWN";
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
}
