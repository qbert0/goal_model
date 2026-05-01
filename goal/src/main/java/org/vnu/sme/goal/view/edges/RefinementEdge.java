package org.vnu.sme.goal.view.edges;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;

import org.tzi.use.gui.views.diagrams.DiagramView;
import org.tzi.use.gui.views.diagrams.elements.EdgeProperty;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.tzi.use.gui.views.diagrams.elements.edges.EdgeBase;
import org.tzi.use.gui.views.diagrams.waypoints.WayPoint;
import org.vnu.sme.goal.mm.Refinement;

public class RefinementEdge extends EdgeBase {
    private final Refinement fRelation;
    
    public RefinementEdge(PlaceableNode source, PlaceableNode target, 
                          Refinement relation, DiagramView diagram) {
        super(source, target, relation.getName(), diagram.getOptions(), true);
        this.fRelation = relation;
    }
    
    @Override
    protected void onDraw(Graphics2D g) {
        if (isSelected()) {
            g.setColor(fOpt.getEDGE_SELECTED_COLOR());
        } else {
            g.setColor(fOpt.getEDGE_COLOR());
        }
        
        drawEdge(g);
    }
    
    private void drawEdge(Graphics2D g) {
        EdgeProperty n1 = null;
        Point2D p1 = null;
        WayPoint n2 = null;
        Point2D p2 = null;
        
        if (!fWayPoints.isEmpty()) {
            Iterator<WayPoint> it = fWayPoints.iterator();
            
            if (it.hasNext()) {
                n1 = it.next();
            }
            
            while (it.hasNext()) {
                n2 = it.next();
            }
            
            p1 = n1.getCenter();
            p2 = n2.getCenter();
            
            try {
                drawRefinementKind(g, p1, p2);
                
                // Vẽ nhãn loại refinement (AND/OR)
                String label = fRelation.getRefinementType() ==
                    Refinement.RefinementType.AND ? "AND" : "OR";
                
                int xCenter = (int) (p1.getX() + p2.getX()) / 2;
                int yCenter = (int) (p1.getY() + p2.getY()) / 2;
                
                FontMetrics fm = g.getFontMetrics();
                int labelWidth = fm.stringWidth(label);
                
                g.setColor(Color.WHITE);
                g.fillRect(xCenter - labelWidth/2 - 3, yCenter - fm.getAscent() - 3, 
                          labelWidth + 6, fm.getHeight() + 6);
                
                g.setColor(isSelected() ? fOpt.getEDGE_SELECTED_COLOR() : fOpt.getEDGE_COLOR());
                g.drawRect(xCenter - labelWidth/2 - 3, yCenter - fm.getAscent() - 3, 
                          labelWidth + 6, fm.getHeight() + 6);
                g.drawString(label, xCenter - labelWidth/2, yCenter);
                
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    protected void drawRefinementKind(Graphics g, Point2D p2d1, Point2D p2d2) {
        Point p1 = new Point((int) Math.round(p2d1.getX()), (int) Math.round(p2d1.getY()));
        Point p2 = new Point((int) Math.round(p2d2.getX()), (int) Math.round(p2d2.getY()));
        
        try {
            // Vẽ đường thẳng với mũi tên T ở cuối
            drawTArrow(g, p1, p2);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void drawTArrow(Graphics g, Point p1, Point p2) {
        // Vẽ đường chính
        g.drawLine(p1.x, p1.y, p2.x, p2.y);
        
        // Vẽ mũi tên T (đường thẳng ngang)
        double angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
        int arrowSize = 15;
        
        int dx = (int) (arrowSize * Math.cos(angle + Math.PI/2));
        int dy = (int) (arrowSize * Math.sin(angle + Math.PI/2));
        
        g.drawLine(p2.x - dx, p2.y - dy, p2.x + dx, p2.y + dy);
    }
    
    @Override
    protected String getStoreType() {
        return "Refinement";
    }
    
    @Override
    public boolean isLink() {
        return false;
    }
    
    @Override
    protected String getIdInternal() {
        return fRelation.getName();
    }
}
