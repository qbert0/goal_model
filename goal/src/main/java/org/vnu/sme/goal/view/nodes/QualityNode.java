package org.vnu.sme.goal.view.nodes;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.tzi.use.gui.views.diagrams.DiagramOptions;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.vnu.sme.goal.mm.Quality;

public class QualityNode extends PlaceableNode {
    private static final int MARGIN = 10;
    private DiagramOptions fOpt;
    private Quality fQuality;
    private String fLabel;
    
    public QualityNode(Quality quality, DiagramOptions opt) {
        this.fOpt = opt;
        this.fQuality = quality;
        this.fLabel = quality.getName();
        setResizeAllowed(true);
    }
    
    @Override
    protected void onDraw(Graphics2D g) {
        Rectangle2D bounds = getBounds();
        FontMetrics fm = g.getFontMetrics();
        
        // Vẽ hình đám mây cho Quality
        g.setColor(new Color(230, 200, 255));
        
        int x = (int) bounds.getX();
        int y = (int) bounds.getY();
        int w = (int) bounds.getWidth();
        int h = (int) bounds.getHeight();
        
        // Vẽ cloud shape
        drawCloud(g, x, y, w, h);
        
        if (isSelected()) {
            g.setColor(fOpt.getNODE_SELECTED_COLOR());
        } else {
            g.setColor(Color.MAGENTA.darker());
        }
        drawCloudOutline(g, x, y, w, h);
        
        // Vẽ label
        g.setColor(Color.BLACK);
        String typeStr = "<<Quality>>";
        int typeWidth = fm.stringWidth(typeStr);
        g.drawString(typeStr,
            (int)(bounds.getCenterX() - typeWidth / 2),
            (int)(bounds.getCenterY() - 5));
        
        int labelWidth = fm.stringWidth(fLabel);
        g.drawString(fLabel, 
            (int)(bounds.getCenterX() - labelWidth / 2),
            (int)(bounds.getCenterY() + fm.getAscent() + 5));
    }
    
    private void drawCloud(Graphics2D g, int x, int y, int w, int h) {
        // Vẽ đám mây đơn giản bằng nhiều hình oval
        g.fillOval(x + w/4, y + h/3, w/3, h/2);
        g.fillOval(x + w/2, y + h/4, w/3, h/2);
        g.fillOval(x + w/3, y + h/2, w/2, h/2);
    }
    
    private void drawCloudOutline(Graphics2D g, int x, int y, int w, int h) {
        g.drawOval(x + w/4, y + h/3, w/3, h/2);
        g.drawOval(x + w/2, y + h/4, w/3, h/2);
        g.drawOval(x + w/3, y + h/2, w/2, h/2);
    }
    
    @Override
    public void doCalculateSize(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = fm.stringWidth(fLabel);
        int typeWidth = fm.stringWidth("<<Quality>>");
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
        return "Quality";
    }

    @Override
    public boolean isResizable() {
        return true;
    }
}
