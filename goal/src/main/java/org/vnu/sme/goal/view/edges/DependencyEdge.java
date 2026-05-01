package org.vnu.sme.goal.view.edges;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;

import org.tzi.use.gui.views.diagrams.DiagramView;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.tzi.use.gui.views.diagrams.elements.edges.EdgeBase;
import org.vnu.sme.goal.mm.Dependency;

public class DependencyEdge extends EdgeBase {
    private Dependency fDependency;
    private PlaceableNode fDependumNode;
    
    public DependencyEdge(PlaceableNode depender, PlaceableNode dependee,
                          PlaceableNode dependum, Dependency dependency, DiagramView diagram) {
        super(depender, dependee, dependency.getName(), diagram.getOptions(), true);
        this.fDependency = dependency;
        this.fDependumNode = dependum;
    }
    
    @Override
    protected void onDraw(Graphics2D g) {
        if (isSelected()) {
            g.setColor(fOpt.getEDGE_SELECTED_COLOR());
        } else {
            g.setColor(Color.BLUE);
        }
        
        drawDependency(g);
    }
    
    private void drawDependency(Graphics2D g) {
        Point2D dependerCenter = fSource.getCenter();
        Point2D dependeeCenter = fTarget.getCenter();
        Point2D dependumCenter = fDependumNode.getCenter();
        
        // Vẽ đường từ depender đến dependum
        drawArrowLine(g, dependerCenter, dependumCenter, "D");
        
        // Vẽ đường từ dependum đến dependee
        drawArrowLine(g, dependumCenter, dependeeCenter, "");
        
        // Vẽ nhãn dependency
        if (fDependency.getName() != null && !fDependency.getName().isEmpty()) {
            FontMetrics fm = g.getFontMetrics();
            int labelWidth = fm.stringWidth(fDependency.getName());
            
            double midX = (dependerCenter.getX() + dependeeCenter.getX()) / 2;
            double midY = (dependerCenter.getY() + dependeeCenter.getY()) / 2 - 20;
            
            g.setColor(Color.WHITE);
            g.fillRect((int)midX - labelWidth/2 - 3, (int)midY - fm.getAscent() - 3,
                      labelWidth + 6, fm.getHeight() + 6);
            
            g.setColor(Color.BLUE);
            g.drawRect((int)midX - labelWidth/2 - 3, (int)midY - fm.getAscent() - 3,
                      labelWidth + 6, fm.getHeight() + 6);
            g.drawString(fDependency.getName(), (int)midX - labelWidth/2, (int)midY);
        }
    }
    
    private void drawArrowLine(Graphics g, Point2D start, Point2D end, String label) {
        Point p1 = new Point((int) start.getX(), (int) start.getY());
        Point p2 = new Point((int) end.getX(), (int) end.getY());
        
        // Vẽ đường
        g.drawLine(p1.x, p1.y, p2.x, p2.y);
        
        // Vẽ mũi tên
        double angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
        int arrowSize = 12;
        
        int xArrow1 = p2.x - (int) (arrowSize * Math.cos(angle - Math.PI/8));
        int yArrow1 = p2.y - (int) (arrowSize * Math.sin(angle - Math.PI/8));
        int xArrow2 = p2.x - (int) (arrowSize * Math.cos(angle + Math.PI/8));
        int yArrow2 = p2.y - (int) (arrowSize * Math.sin(angle + Math.PI/8));
        
        g.drawLine(p2.x, p2.y, xArrow1, yArrow1);
        g.drawLine(p2.x, p2.y, xArrow2, yArrow2);
        
        // Vẽ label nếu có
        if (!label.isEmpty() && g instanceof Graphics2D) {
            Graphics2D g2d = (Graphics2D) g;
            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(label);
            
            int midX = (p1.x + p2.x) / 2;
            int midY = (p1.y + p2.y) / 2 - 10;
            
            g2d.setColor(Color.YELLOW);
            g2d.fillOval(midX - labelWidth/2 - 5, midY - fm.getAscent() - 5,
                        labelWidth + 10, fm.getHeight() + 10);
            
            g2d.setColor(Color.BLUE);
            g2d.drawOval(midX - labelWidth/2 - 5, midY - fm.getAscent() - 5,
                        labelWidth + 10, fm.getHeight() + 10);
            g2d.drawString(label, midX - labelWidth/2, midY);
        }
    }
    
    @Override
    protected String getStoreType() {
        return "Dependency";
    }
    
    @Override
    public boolean isLink() {
        return false;
    }
    
    @Override
    protected String getIdInternal() {
        return fDependency.getName();
    }
}