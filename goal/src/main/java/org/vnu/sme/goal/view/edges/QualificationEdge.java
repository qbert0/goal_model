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
import org.vnu.sme.goal.mm.QualificationRelation;

public class QualificationEdge extends EdgeBase {
    private QualificationRelation fRelation;
    
    public QualificationEdge(PlaceableNode source, PlaceableNode target,
                             QualificationRelation relation, DiagramView diagram) {
        super(source, target, relation.getName(), diagram.getOptions(), true);
        this.fRelation = relation;
    }
    
    @Override
    protected void onDraw(Graphics2D g) {
        if (isSelected()) {
            g.setColor(fOpt.getEDGE_SELECTED_COLOR());
        } else {
            g.setColor(Color.BLUE);
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
                drawQualificationKind(g, p1, p2);
                
                // Vẽ nhãn "Q"
                String label = "Q";
                
                int xCenter = (int) (p1.getX() + p2.getX()) / 2;
                int yCenter = (int) (p1.getY() + p2.getY()) / 2;
                
                FontMetrics fm = g.getFontMetrics();
                int labelWidth = fm.stringWidth(label);
                
                // Vẽ hình tròn cho nhãn
                g.setColor(Color.WHITE);
                g.fillOval(xCenter - labelWidth/2 - 5, yCenter - fm.getAscent() - 5,
                          labelWidth + 10, fm.getHeight() + 10);
                
                g.setColor(isSelected() ? fOpt.getEDGE_SELECTED_COLOR() : Color.BLUE);
                g.drawOval(xCenter - labelWidth/2 - 5, yCenter - fm.getAscent() - 5,
                          labelWidth + 10, fm.getHeight() + 10);
                g.drawString(label, xCenter - labelWidth/2, yCenter);
                
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    protected void drawQualificationKind(Graphics g, Point2D p2d1, Point2D p2d2) {
        Point p1 = new Point((int) Math.round(p2d1.getX()), (int) Math.round(p2d1.getY()));
        Point p2 = new Point((int) Math.round(p2d2.getX()), (int) Math.round(p2d2.getY()));
        
        try {
            // Vẽ đường với mũi tên rỗng
            drawOpenArrow(g, p1, p2);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private void drawOpenArrow(Graphics g, Point p1, Point p2) {
        // Vẽ đường chính
        g.drawLine(p1.x, p1.y, p2.x, p2.y);
        
        // Vẽ mũi tên rỗng
        double angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
        int arrowSize = 12;
        
        int xArrow1 = p2.x - (int) (arrowSize * Math.cos(angle - Math.PI/6));
        int yArrow1 = p2.y - (int) (arrowSize * Math.sin(angle - Math.PI/6));
        int xArrow2 = p2.x - (int) (arrowSize * Math.cos(angle + Math.PI/6));
        int yArrow2 = p2.y - (int) (arrowSize * Math.sin(angle + Math.PI/6));
        
        g.drawLine(p2.x, p2.y, xArrow1, yArrow1);
        g.drawLine(p2.x, p2.y, xArrow2, yArrow2);
    }
    
    @Override
    protected String getStoreType() {
        return "Qualification";
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