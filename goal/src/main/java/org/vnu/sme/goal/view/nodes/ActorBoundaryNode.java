package org.vnu.sme.goal.view.nodes;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

import org.tzi.use.gui.views.diagrams.DiagramOptions;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.vnu.sme.goal.mm.Actor;

public class ActorBoundaryNode extends PlaceableNode {
    private static final int PADDING = 28;
    private static final int MIN_WIDTH = 180;
    private static final int MIN_HEIGHT = 150;
    private static final int HIT_SIZE = 10;
    private static final int HEADER_HEIGHT = 24;

    private final Actor actor;
    private final DiagramOptions opt;
    private final List<PlaceableNode> ownedNodes = new ArrayList<>();

    public ActorBoundaryNode(Actor actor, DiagramOptions opt) {
        this.actor = actor;
        this.opt = opt;
        setResizeAllowed(true);
    }

    public Actor getActor() {
        return actor;
    }

    public void addOwnedNode(PlaceableNode node) {
        ownedNodes.add(node);
    }

    public void updateFromOwnedNodes() {
        if (ownedNodes.isEmpty()) {
            return;
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;

        for (PlaceableNode node : ownedNodes) {
            Rectangle2D bounds = node.getBounds();
            minX = Math.min(minX, bounds.getMinX());
            minY = Math.min(minY, bounds.getMinY());
            maxX = Math.max(maxX, bounds.getMaxX());
            maxY = Math.max(maxY, bounds.getMaxY());
        }

        setPosition(minX - PADDING, minY - PADDING);
        setExactBounds(Math.max(MIN_WIDTH, maxX - minX + 2 * PADDING),
                Math.max(MIN_HEIGHT, maxY - minY + 2 * PADDING));
    }

    @Override
    protected void onDraw(Graphics2D g) {
        Rectangle2D bounds = getBounds();
        Color oldColor = g.getColor();
        java.awt.Stroke oldStroke = g.getStroke();

        g.setColor(new Color(248, 250, 252, 120));
        g.fillRoundRect((int) bounds.getX(), (int) bounds.getY(),
                (int) bounds.getWidth(), (int) bounds.getHeight(), 18, 18);

        g.setColor(isSelected() ? opt.getNODE_SELECTED_COLOR() : new Color(95, 110, 130));
        g.setStroke(new BasicStroke(1.3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
                1.0f, new float[] {7.0f, 5.0f}, 0.0f));
        g.drawRoundRect((int) bounds.getX(), (int) bounds.getY(),
                (int) bounds.getWidth(), (int) bounds.getHeight(), 18, 18);

        g.setStroke(oldStroke);
        g.setColor(new Color(42, 52, 65));
        g.drawString(actor.getType() + " boundary: " + actor.getName(),
                (int) bounds.getX() + 12, (int) bounds.getY() + 18);
        g.setColor(oldColor);
    }

    @Override
    public void doCalculateSize(Graphics2D g) {
        if (getWidth() == 0 || getHeight() == 0) {
            setCalculatedBounds(MIN_WIDTH, MIN_HEIGHT);
        }
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public boolean occupies(double x, double y) {
        Rectangle2D bounds = getBounds();
        if (!bounds.contains(x, y)) {
            return false;
        }

        boolean inHeader = y <= bounds.getY() + HEADER_HEIGHT;
        boolean nearLeft = x <= bounds.getX() + HIT_SIZE;
        boolean nearRight = x >= bounds.getMaxX() - HIT_SIZE;
        boolean nearTop = y <= bounds.getY() + HIT_SIZE;
        boolean nearBottom = y >= bounds.getMaxY() - HIT_SIZE;
        return inHeader || nearLeft || nearRight || nearTop || nearBottom;
    }

    @Override
    public String name() {
        return actor.getName() + " boundary";
    }

    @Override
    public String getId() {
        return actor.getName() + "::boundary";
    }

    @Override
    protected String getStoreType() {
        return "ActorBoundary";
    }
}
