package org.vnu.sme.goal.view.edges;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;

import org.tzi.use.gui.views.diagrams.DiagramView;
import org.tzi.use.gui.views.diagrams.edges.DirectedEdgeFactory;
import org.tzi.use.gui.views.diagrams.elements.EdgeProperty;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.tzi.use.gui.views.diagrams.elements.edges.EdgeBase;
import org.tzi.use.gui.views.diagrams.waypoints.WayPoint;
import org.vnu.sme.goal.mm.Contribution;
import org.vnu.sme.goal.mm.ContributionType;

public class ContributionEdge extends EdgeBase {
    private final Contribution relation;
    private final String edgeId;

    public ContributionEdge(PlaceableNode source, PlaceableNode target, Contribution relation, DiagramView diagram) {
        super(source, target, relation.getName(), diagram.getOptions(), true);
        this.relation = relation;
        this.edgeId = relation.getName() + "::" + source.getId() + "->" + target.getId();
    }

    @Override
    protected void onDraw(Graphics2D g) {
        g.setColor(isSelected() ? fOpt.getEDGE_SELECTED_COLOR() : colorFor(relation.getContributionType()));
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
            DirectedEdgeFactory.drawDirectedEdge(g, source.x, source.y, target.x, target.y, false);
            drawLabel(g, p1, p2, labelFor(relation.getContributionType()));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void drawLabel(Graphics2D g, Point2D p1, Point2D p2, String label) {
        FontMetrics metrics = g.getFontMetrics();
        int width = metrics.stringWidth(label);
        int centerX = (int) ((p1.getX() + p2.getX()) / 2.0);
        int centerY = (int) ((p1.getY() + p2.getY()) / 2.0);

        Color oldColor = g.getColor();
        g.setColor(Color.WHITE);
        g.fillRect(centerX - width / 2 - 4, centerY - metrics.getAscent() - 3, width + 8, metrics.getHeight() + 6);
        g.setColor(Color.BLACK);
        g.drawRect(centerX - width / 2 - 4, centerY - metrics.getAscent() - 3, width + 8, metrics.getHeight() + 6);
        g.drawString(label, centerX - width / 2, centerY);
        g.setColor(oldColor);
    }

    private static String labelFor(ContributionType type) {
        return switch (type) {
            case MAKE -> "make";
            case HELP -> "help";
            case HURT -> "hurt";
            case BREAK -> "break";
            case SOME_PLUS -> "some+";
            case SOME_MINUS -> "some-";
            case UNKNOWN -> "unknown";
        };
    }

    private static Color colorFor(ContributionType type) {
        return switch (type) {
            case MAKE, HELP, SOME_PLUS -> new Color(0, 110, 0);
            case HURT, BREAK, SOME_MINUS -> new Color(160, 0, 0);
            case UNKNOWN -> Color.DARK_GRAY;
        };
    }

    @Override
    protected String getStoreType() {
        return "Contribution";
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
