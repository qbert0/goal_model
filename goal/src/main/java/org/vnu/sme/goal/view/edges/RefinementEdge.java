package org.vnu.sme.goal.view.edges;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.geom.Point2D;
import java.util.Iterator;

import org.tzi.use.gui.views.diagrams.DiagramView;
import org.tzi.use.gui.views.diagrams.edges.DirectedEdgeFactory;
import org.tzi.use.gui.views.diagrams.elements.EdgeProperty;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.tzi.use.gui.views.diagrams.elements.edges.EdgeBase;
import org.tzi.use.gui.views.diagrams.waypoints.WayPoint;
import org.vnu.sme.goal.mm.Refinement;

public class RefinementEdge extends EdgeBase {
    private final Refinement relation;
    private final String edgeId;

    public RefinementEdge(PlaceableNode source, PlaceableNode target, Refinement relation, DiagramView diagram) {
        super(source, target, relation.getName(), diagram.getOptions(), true);
        this.relation = relation;
        this.edgeId = relation.getName() + "::" + source.getId() + "->" + target.getId();
    }

    @Override
    protected void onDraw(Graphics2D g) {
        g.setColor(isSelected() ? fOpt.getEDGE_SELECTED_COLOR() : fOpt.getEDGE_COLOR());
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

        try {
            if (relation.getRefinementType() == Refinement.RefinementType.AND) {
                drawTArrow(g, source, target);
            } else {
                drawFilledArrow(g, source, target);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void drawTArrow(Graphics2D g, Point source, Point target) {
        g.drawLine(source.x, source.y, target.x, target.y);

        double angle = Math.atan2(target.y - source.y, target.x - source.x);
        int barSize = 15;
        int dx = (int) (barSize * Math.cos(angle + Math.PI / 2));
        int dy = (int) (barSize * Math.sin(angle + Math.PI / 2));
        g.drawLine(target.x - dx, target.y - dy, target.x + dx, target.y + dy);
    }

    private void drawFilledArrow(Graphics2D g, Point source, Point target) throws Exception {
        Stroke oldStroke = g.getStroke();
        g.setStroke(new BasicStroke(2.2f));
        DirectedEdgeFactory.drawArrow(g, source.x, source.y, target.x, target.y, DirectedEdgeFactory.ArrowStyle.FILLED);
        g.setStroke(oldStroke);
    }

    @Override
    protected String getStoreType() {
        return "Refinement";
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
