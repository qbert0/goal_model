package org.vnu.sme.goal.view.nodes;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import org.tzi.use.gui.views.diagrams.DiagramOptions;
import org.tzi.use.gui.views.diagrams.ToolTipProvider;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.vnu.sme.goal.mm.Task;

public class TaskNode extends PlaceableNode implements ToolTipProvider {
    private static final int MARGIN = 10;
    private DiagramOptions fOpt;
    private Task fTask;
    private String fLabel;
    
    public TaskNode(Task task, DiagramOptions opt) {
        this.fOpt = opt;
        this.fTask = task;
        this.fLabel = task.getName();
        setResizeAllowed(true);
    }
    
    @Override
    protected void onDraw(Graphics2D g) {
        Rectangle2D bounds = getBounds();
        FontMetrics fm = g.getFontMetrics();
        
        // Vẽ hình lục giác cho Task
        int x = (int) bounds.getX();
        int y = (int) bounds.getY();
        int w = (int) bounds.getWidth();
        int h = (int) bounds.getHeight();
        int hexOffset = w / 4;
        
        Polygon hexagon = new Polygon();
        hexagon.addPoint(x + hexOffset, y);
        hexagon.addPoint(x + w - hexOffset, y);
        hexagon.addPoint(x + w, y + h/2);
        hexagon.addPoint(x + w - hexOffset, y + h);
        hexagon.addPoint(x + hexOffset, y + h);
        hexagon.addPoint(x, y + h/2);
        
        g.setColor(new Color(255, 230, 200));
        g.fillPolygon(hexagon);
        
        if (isSelected()) {
            g.setColor(fOpt.getNODE_SELECTED_COLOR());
        } else {
            g.setColor(Color.ORANGE.darker());
        }
        g.drawPolygon(hexagon);
        
        // Vẽ label
        g.setColor(Color.BLACK);
        String typeStr = "<<Task>>";
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
        int typeWidth = fm.stringWidth("<<Task>>");
        int width = Math.max(Math.max(labelWidth, typeWidth), 80) + 2 * MARGIN;
        int height = 70;
        
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
        return "Task";
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public String getToolTip(MouseEvent event) {
        StringBuilder tooltip = new StringBuilder("<html><b>").append(fLabel).append("</b>");
        if (fTask.getPreExpression() != null) {
            tooltip.append("<br/>pre: ").append(escape(fTask.getPreExpression()));
        }
        if (fTask.getPostExpression() != null) {
            tooltip.append("<br/>post: ").append(escape(fTask.getPostExpression()));
        }
        return tooltip.append("</html>").toString();
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
    
    public Task getTask() {
        return fTask;
    }
}
