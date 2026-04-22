package org.vnu.sme.goal.view.nodes;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;

import org.tzi.use.gui.views.diagrams.DiagramOptions;
import org.tzi.use.gui.views.diagrams.ToolTipProvider;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.vnu.sme.goal.mm.Goal;

public class GoalNode extends PlaceableNode implements ToolTipProvider {
    private static final int MARGIN = 10;
    private DiagramOptions fOpt;
    private Goal fGoal;
    private String fLabel;
    
    public GoalNode(Goal goal, DiagramOptions opt) {
        this.fOpt = opt;
        this.fGoal = goal;
        this.fLabel = goal.getName();
        setResizeAllowed(true);
    }
    
    @Override
    protected void onDraw(Graphics2D g) {
        Rectangle2D bounds = getBounds();
        FontMetrics fm = g.getFontMetrics();
        
        // Vẽ hình oval cho Goal
        g.setColor(new Color(200, 230, 255));
        g.fillOval((int)bounds.getX(), (int)bounds.getY(), 
                   (int)bounds.getWidth(), (int)bounds.getHeight());
        
        if (isSelected()) {
            g.setColor(fOpt.getNODE_SELECTED_COLOR());
        } else {
            g.setColor(Color.BLUE);
        }
        g.drawOval((int)bounds.getX(), (int)bounds.getY(), 
                   (int)bounds.getWidth(), (int)bounds.getHeight());
        
        // Vẽ label
        g.setColor(Color.BLACK);
        int labelWidth = fm.stringWidth(fLabel);
        g.drawString(fLabel, 
            (int)(bounds.getCenterX() - labelWidth / 2),
            (int)(bounds.getCenterY() + fm.getAscent() / 2));
        
        // Vẽ loại goal nếu có
        if (fGoal.getGoalType() != null) {
            String typeStr = "<<" + fGoal.getGoalType().toString().toLowerCase() + ">>";
            int typeWidth = fm.stringWidth(typeStr);
            g.drawString(typeStr,
                (int)(bounds.getCenterX() - typeWidth / 2),
                (int)(bounds.getY() + fm.getAscent() + 5));
        }

    }
    
    @Override
    public void doCalculateSize(Graphics2D g) {
        FontMetrics fm = g.getFontMetrics();
        int labelWidth = fm.stringWidth(fLabel);
        int width = Math.max(labelWidth + 2 * MARGIN, 100);
        int height = 60;
        
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
        return "Goal";
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public String getToolTip(MouseEvent event) {
        StringBuilder tooltip = new StringBuilder("<html><b>").append(fLabel).append("</b>");
        if (fGoal.getOclExpression() != null) {
            tooltip.append("<br/>OCL: ").append(escape(fGoal.getOclExpression()));
        }
        return tooltip.append("</html>").toString();
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
