package org.vnu.sme.goal.view.nodes;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.tzi.use.gui.views.diagrams.DiagramOptions;
import org.tzi.use.gui.views.diagrams.behavior.DrawingUtil;
import org.vnu.sme.goal.mm.Role;

public class RoleNode extends ActorNode {
    private static final int SMALL_ROLE_HEIGHT = 47;
    
    public RoleNode(Role role, DiagramOptions opt) {
        super(role, opt);
    }
    
    @Override
    protected void onDraw(Graphics2D g) {
        Color oldColor = g.getColor();
        Rectangle2D currentBounds = getBounds();
        FontMetrics fm = g.getFontMetrics();
        
        // Role có màu nền khác
        g.setColor(new Color(255, 220, 220));
        g.fill(currentBounds);
        
        if (isSelected()) {
            g.setColor(fOpt.getNODE_SELECTED_COLOR());
        } else {
            g.setColor(fOpt.getEDGE_COLOR());
        }
        
        // Vẽ dashed border cho Role
        float[] dashPattern = {5.0f, 5.0f};
        g.setStroke(new java.awt.BasicStroke(1.0f, java.awt.BasicStroke.CAP_BUTT, 
                     java.awt.BasicStroke.JOIN_MITER, 10.0f, dashPattern, 0.0f));
        g.draw(currentBounds);
        g.setStroke(new java.awt.BasicStroke(1.0f));
        
        int x = (int) currentBounds.getCenterX();
        int y = (int) currentBounds.getY();
        
        // Vẽ actor nhỏ hơn cho Role
        drawSmallRole(g, x, y + MARGIN);
        
        g.setColor(Color.BLACK);
        String typeStr = "<<Role>>";
        int typeWidth = fm.stringWidth(typeStr);
        g.drawString(typeStr, 
            (int) (currentBounds.getCenterX() - typeWidth / 2), 
            (int) currentBounds.getMinY() + SMALL_ROLE_HEIGHT + fm.getAscent() + 2 * MARGIN);
        
        int labelWidth = fm.stringWidth(fLabel);
        g.drawString(fLabel, 
            (int) (currentBounds.getCenterX() - labelWidth / 2), 
            (int) currentBounds.getMinY() + SMALL_ROLE_HEIGHT + 2 * fm.getAscent() + 3 * MARGIN);
        
        g.setColor(oldColor);
    }
    
    private void drawSmallRole(Graphics2D g, int x, int y) {
        // Vẽ actor nhỏ cho Role
        int headSize = 12;
        int bodyHeight = 20;
        int armLength = 20;
        
        // Đầu
        g.fillOval(x - headSize/2, y, headSize, headSize);
        
        // Thân
        g.drawLine(x, y + headSize, x, y + headSize + bodyHeight);
        
        // Tay
        g.drawLine(x - armLength/2, y + headSize + bodyHeight/3, 
                   x + armLength/2, y + headSize + bodyHeight/3);
        
        // Chân
        g.drawLine(x, y + headSize + bodyHeight, 
                   x - armLength/3, y + headSize + bodyHeight + 15);
        g.drawLine(x, y + headSize + bodyHeight, 
                   x + armLength/3, y + headSize + bodyHeight + 15);
    }
    
    @Override
    public void doCalculateSize(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = fm.stringWidth(fLabel);
        int typeWidth = fm.stringWidth("<<Role>>");
        int maxWidth = Math.max(Math.max(labelWidth, typeWidth), 40);
        
        setCalculatedBounds(
            maxWidth + 2 * MARGIN, 
            2 * fm.getHeight() + 60 + 4 * MARGIN
        );
    }
    
    @Override
    protected String getStoreType() {
        return "Role";
    }
}
