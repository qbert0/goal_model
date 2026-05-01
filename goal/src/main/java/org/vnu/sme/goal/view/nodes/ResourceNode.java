package org.vnu.sme.goal.view.nodes;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.tzi.use.gui.views.diagrams.DiagramOptions;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.vnu.sme.goal.mm.Resource;

public class ResourceNode extends PlaceableNode {
    private static final int MARGIN = 10;
    private DiagramOptions fOpt;
    private Resource fResource;
    private String fLabel;
    
    public ResourceNode(Resource resource, DiagramOptions opt) {
        this.fOpt = opt;
        this.fResource = resource;
        this.fLabel = resource.getName();
        setResizeAllowed(true);
    }
    
    @Override
    protected void onDraw(Graphics2D g) {
        Rectangle2D bounds = getBounds();
        FontMetrics fm = g.getFontMetrics();
        
        // Vẽ hình chữ nhật cho Resource
        g.setColor(new Color(200, 255, 200));
        g.fillRect((int)bounds.getX(), (int)bounds.getY(), 
                   (int)bounds.getWidth(), (int)bounds.getHeight());
        
        if (isSelected()) {
            g.setColor(fOpt.getNODE_SELECTED_COLOR());
        } else {
            g.setColor(Color.GREEN.darker());
        }
        g.drawRect((int)bounds.getX(), (int)bounds.getY(), 
                   (int)bounds.getWidth(), (int)bounds.getHeight());
        
        // Vẽ label
        g.setColor(Color.BLACK);
        String typeStr = "<<Resource>>";
        int typeWidth = fm.stringWidth(typeStr);
        g.drawString(typeStr,
            (int)(bounds.getCenterX() - typeWidth / 2),
            (int)(bounds.getCenterY() - 5));
        
        int labelWidth = fm.stringWidth(fLabel);
        g.drawString(fLabel, 
            (int)(bounds.getCenterX() - labelWidth / 2),
            (int)(bounds.getCenterY() + fm.getAscent() + 5));
    }
    
    @Override
    public void doCalculateSize(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = fm.stringWidth(fLabel);
        int typeWidth = fm.stringWidth("<<Resource>>");
        int width = Math.max(Math.max(labelWidth, typeWidth), 80) + 2 * MARGIN;
        int height = 65;
        
        setCalculatedBounds(width, height);
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
        return "Resource";
    }

    @Override
    public boolean isResizable() {
        return true;
    }
}
