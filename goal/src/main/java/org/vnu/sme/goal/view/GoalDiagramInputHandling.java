package org.vnu.sme.goal.view;

import java.awt.Point;
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

    public GoalDiagramInputHandling(Selection<PlaceableNode> nodeSelection,
                                    Selection<EdgeBase> edgeSelection,
                                    GoalDiagramView diagram) {
        super(nodeSelection, edgeSelection, diagram);
        this.goalDiagram = diagram;
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
}
