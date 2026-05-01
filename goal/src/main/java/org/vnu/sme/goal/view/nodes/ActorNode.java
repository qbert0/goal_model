package org.vnu.sme.goal.view.nodes;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.tzi.use.gui.views.diagrams.DiagramOptions;
import org.tzi.use.gui.views.diagrams.behavior.DrawingUtil;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.vnu.sme.goal.mm.Actor;

public class ActorNode extends PlaceableNode {
    protected static final int MARGIN = 5;
    protected DiagramOptions fOpt;
    protected Actor fActor;
    protected String fLabel;
    
    public ActorNode(Actor actor, DiagramOptions opt) {
        this.fOpt = opt;
        this.fActor = actor;
        this.fLabel = actor.getName();
        setResizeAllowed(true);
    }
    
    @Override
    protected void onDraw(Graphics2D g) {
        Color oldColor = g.getColor();
        Rectangle2D currentBounds = getBounds();
        FontMetrics fm = g.getFontMetrics();
        
        g.setColor(new Color(255, 255, 200));
        g.fill(currentBounds);
        
        if (isSelected()) {
            g.setColor(fOpt.getNODE_SELECTED_COLOR());
        } else {
            g.setColor(fOpt.getEDGE_COLOR());
        }
        g.draw(currentBounds);
        
        int x = (int) currentBounds.getCenterX();
        int y = (int) currentBounds.getY();
        
        DrawingUtil.drawBigActor(x, y + MARGIN, g);
        g.setColor(Color.BLACK);
        g.drawString(fLabel, 
            (int) (currentBounds.getCenterX() - fm.stringWidth(fLabel) / 2), 
            (int) currentBounds.getMinY() + DrawingUtil.TOTAL_HEIGHT_BIG + fm.getAscent() + 2 * MARGIN);
        
        g.setColor(oldColor);
    }
    
    @Override
    public void doCalculateSize(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = fm.stringWidth(fLabel);
        int maxWidth = Math.max(labelWidth, DrawingUtil.ARMS_LENGTH_BIG);
        
        setCalculatedBounds(
            maxWidth + 2 * MARGIN, 
            fm.getHeight() + DrawingUtil.TOTAL_HEIGHT_BIG + 3 * MARGIN
        );
    }
    
    @Override
    public String name() {
        return fLabel;
    }
    
    @Override
    public String getId() {
        return fLabel;
    }
    
    @Override
    protected String getStoreType() {
        return "Actor";
    }

    @Override
    public boolean isResizable() {
        return true;
    }
    
    public Actor getActor() {
        return fActor;
    }
}
