package org.vnu.sme.goal.view.edges;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;

import org.tzi.use.gui.views.diagrams.DiagramView;
import org.tzi.use.gui.views.diagrams.elements.EdgeProperty;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.tzi.use.gui.views.diagrams.elements.edges.EdgeBase;
import org.tzi.use.gui.views.diagrams.waypoints.WayPoint;

public class NeededByEdge extends EdgeBase {
    private final String edgeId;

    public NeededByEdge(PlaceableNode source, PlaceableNode target, String edgeId, DiagramView diagram) {
        super(source, target, edgeId, diagram.getOptions(), true);
        this.edgeId = edgeId;
    }

    @Override
    protected void onDraw(Graphics2D g) {
        g.setColor(isSelected() ? fOpt.getEDGE_SELECTED_COLOR() : Color.BLACK);
        drawEdge(g);
    }

    private void drawEdge(Graphics2D g) {
        if (fWayPoints.isEmpty()) {
            return;
        }

        Iterator<WayPoint> iterator = fWayPoints.iterator();
        EdgeProperty first = null;
        WayPoint last = null;

        if (iterator.hasNext()) {
            first = iterator.next();
        }
        while (iterator.hasNext()) {
            last = iterator.next();
        }
        if (first == null || last == null) {
            return;
        }

        Point2D p1 = first.getCenter();
        Point2D p2 = last.getCenter();
        Point source = new Point((int) Math.round(p1.getX()), (int) Math.round(p1.getY()));
        Point target = new Point((int) Math.round(p2.getX()), (int) Math.round(p2.getY()));

        g.drawLine(source.x, source.y, target.x, target.y);
        g.fillOval(source.x - 5, source.y - 5, 10, 10);
    }

    @Override
    protected String getStoreType() {
        return "NeededBy";
    }

    @Override
    public boolean isLink() {
        return false;
    }

    @Override
    protected String getIdInternal() {
        return edgeId;
    }
}
