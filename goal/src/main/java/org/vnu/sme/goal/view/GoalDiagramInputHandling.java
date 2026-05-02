package org.vnu.sme.goal.view;

import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

import org.tzi.use.gui.util.Selection;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.tzi.use.gui.views.diagrams.elements.edges.EdgeBase;
import org.tzi.use.gui.views.diagrams.event.DiagramInputHandling;
import org.vnu.sme.goal.mm.Actor;
import org.vnu.sme.goal.view.nodes.ActorBoundaryNode;
import org.vnu.sme.goal.view.nodes.ActorNode;

public class GoalDiagramInputHandling extends DiagramInputHandling {
    private final GoalDiagramView goalDiagram;
    private final Selection<PlaceableNode> nodeSelection;
    private final Selection<EdgeBase> edgeSelection;

    public GoalDiagramInputHandling(Selection<PlaceableNode> nodeSelection,
                                    Selection<EdgeBase> edgeSelection,
                                    GoalDiagramView diagram) {
        super(nodeSelection, edgeSelection, diagram);
        this.goalDiagram = diagram;
        this.nodeSelection = nodeSelection;
        this.edgeSelection = edgeSelection;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (handlePopupTrigger(e)) {
            return;
        }
        super.mousePressed(e);
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (handlePopupTrigger(e)) {
            return;
        }
        super.mouseReleased(e);
    }

    @Override
    protected void moveSelectedObjects(int dx, int dy) {
        Set<Actor> movedActors = new HashSet<>();
        for (PlaceableNode node : goalDiagram.getNodeSelection()) {
            if (node instanceof ActorBoundaryNode) {
                Actor actor = ((ActorBoundaryNode) node).getActor();
                if (movedActors.add(actor)) {
                    goalDiagram.moveActorGroup(actor, dx, dy);
                }
            } else if (node instanceof ActorNode) {
                Actor actor = ((ActorNode) node).getActor();
                if (movedActors.add(actor)) {
                    goalDiagram.moveActorGroup(actor, dx, dy);
                }
            } else {
                node.setDraggedPosition(dx, dy);
                if (goalDiagram.getGraph().contains(node)) {
                    goalDiagram.invalidateNode(node);
                    goalDiagram.updateBoundaryForElement(node);
                }
            }
        }
    }

    @Override
    protected void resizeSelectedObjects(Point p) {
        super.resizeSelectedObjects(p);
        for (PlaceableNode node : goalDiagram.getNodeSelection()) {
            goalDiagram.updateBoundaryForElement(node);
        }
    }

    private boolean handlePopupTrigger(MouseEvent e) {
        if (!e.isPopupTrigger()) {
            return false;
        }

        goalDiagram.requestFocusInWindow();
        selectContextAt(e);
        return goalDiagram.maybeShowPopup(e);
    }

    private void selectContextAt(MouseEvent e) {
        PlaceableNode pickedNode = goalDiagram.findNode(e.getX(), e.getY());
        EdgeBase pickedEdge = goalDiagram.findEdge(e.getX(), e.getY());

        if (pickedNode != null && !nodeSelection.isSelected(pickedNode)) {
            nodeSelection.clear();
            edgeSelection.clear();
            nodeSelection.add(pickedNode);
            goalDiagram.repaint();
            return;
        }

        if (pickedEdge != null && !edgeSelection.isSelected(pickedEdge)) {
            nodeSelection.clear();
            edgeSelection.clear();
            edgeSelection.add(pickedEdge);
            goalDiagram.repaint();
            return;
        }

        if (pickedNode == null && pickedEdge == null) {
            nodeSelection.clear();
            edgeSelection.clear();
            goalDiagram.repaint();
        }
    }
}
