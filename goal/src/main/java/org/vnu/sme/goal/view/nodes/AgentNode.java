package org.vnu.sme.goal.view.nodes;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.tzi.use.gui.views.diagrams.DiagramOptions;
import org.tzi.use.gui.views.diagrams.behavior.DrawingUtil;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.vnu.sme.goal.mm.Agent;

public class AgentNode extends ActorNode {
    
    public AgentNode(Agent agent, DiagramOptions opt) {
        super(agent, opt);
    }
    
    @Override
    protected void onDraw(Graphics2D g) {
        Color oldColor = g.getColor();
        Rectangle2D currentBounds = getBounds();
        FontMetrics fm = g.getFontMetrics();
        
        // Agent có màu nền khác
        g.setColor(new Color(220, 255, 220));
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
        
        // Vẽ text "Agent"
        g.setColor(Color.BLACK);
        String typeStr = "<<Agent>>";
        int typeWidth = fm.stringWidth(typeStr);
        g.drawString(typeStr, 
            (int) (currentBounds.getCenterX() - typeWidth / 2), 
            (int) currentBounds.getMinY() + DrawingUtil.TOTAL_HEIGHT_BIG + fm.getAscent() + 2 * MARGIN);
        
        // Vẽ tên
        int labelWidth = fm.stringWidth(fLabel);
        g.drawString(fLabel, 
            (int) (currentBounds.getCenterX() - labelWidth / 2), 
            (int) currentBounds.getMinY() + DrawingUtil.TOTAL_HEIGHT_BIG + 2 * fm.getAscent() + 3 * MARGIN);
        
        g.setColor(oldColor);
    }
    
    @Override
    public void doCalculateSize(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = fm.stringWidth(fLabel);
        int typeWidth = fm.stringWidth("<<Agent>>");
        int maxWidth = Math.max(Math.max(labelWidth, typeWidth), DrawingUtil.ARMS_LENGTH_BIG);
        
        setCalculatedBounds(
            maxWidth + 2 * MARGIN, 
            2 * fm.getHeight() + DrawingUtil.TOTAL_HEIGHT_BIG + 4 * MARGIN
        );
    }
    
    @Override
    protected String getStoreType() {
        return "Agent";
    }
}