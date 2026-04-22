package org.vnu.sme.goal.view.edges;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.Point2D;
import java.util.Iterator;

import org.tzi.use.gui.views.diagrams.DiagramView;
import org.tzi.use.gui.views.diagrams.edges.DirectedEdgeFactory;
import org.tzi.use.gui.views.diagrams.elements.EdgeProperty;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.tzi.use.gui.views.diagrams.elements.edges.EdgeBase;
import org.tzi.use.gui.views.diagrams.waypoints.WayPoint;
import org.vnu.sme.goal.mm.ContributionRelation;

public class ContributionEdge extends EdgeBase {
    private ContributionRelation fRelation;
    
    public ContributionEdge(PlaceableNode source, PlaceableNode target,
                            ContributionRelation relation, DiagramView diagram) {
        super(source, target, relation.getName(), diagram.getOptions(), true);
        this.fRelation = relation;
    }
    
    @Override
    protected void onDraw(Graphics2D g) {
        if (isSelected()) {
            g.setColor(fOpt.getEDGE_SELECTED_COLOR());
        } else {
            // Màu khác nhau dựa trên loại contribution
            switch (fRelation.getContributionType()) {
                case MAKE:
                case HELP:
                    g.setColor(Color.GREEN.darker());
                    break;
                case HURT:
                case BREAK:
                    g.setColor(Color.RED);
                    break;
                default:
                    g.setColor(fOpt.getEDGE_COLOR());
            }
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
                drawContributionKind(g, p1, p2);
                
                // Vẽ nhãn contribution
                String label = getContributionSymbol();
                
                int xCenter = (int) (p1.getX() + p2.getX()) / 2;
                int yCenter = (int) (p1.getY() + p2.getY()) / 2;
                
                FontMetrics fm = g.getFontMetrics();
                int labelWidth = fm.stringWidth(label);
                
                g.setColor(Color.WHITE);
                g.fillRect(xCenter - labelWidth/2 - 3, yCenter - fm.getAscent() - 3,
                          labelWidth + 6, fm.getHeight() + 6);
                
                g.setColor(isSelected() ? fOpt.getEDGE_SELECTED_COLOR() : Color.BLACK);
                g.drawRect(xCenter - labelWidth/2 - 3, yCenter - fm.getAscent() - 3,
                          labelWidth + 6, fm.getHeight() + 6);
                g.drawString(label, xCenter - labelWidth/2, yCenter);
                
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    
    private String getContributionSymbol() {
        switch (fRelation.getContributionType()) {
            case MAKE: return "++";
            case HELP: return "+";
            case SOME_PLUS: return "+?";
            case UNKNOWN: return "?";
            case SOME_MINUS: return "-?";
            case HURT: return "-";
            case BREAK: return "--";
            default: return "";
        }
    }
    
    protected void drawContributionKind(Graphics g, Point2D p2d1, Point2D p2d2) {
        Point p1 = new Point((int) Math.round(p2d1.getX()), (int) Math.round(p2d1.getY()));
        Point p2 = new Point((int) Math.round(p2d2.getX()), (int) Math.round(p2d2.getY()));
        
        try {
            // Vẽ đường đứt nét cho contribution
            boolean isDashed = (fRelation.getContributionType() == ContributionRelation.ContributionType.UNKNOWN);
            DirectedEdgeFactory.drawDirectedEdge(g, p1.x, p1.y, p2.x, p2.y, isDashed);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    protected String getStoreType() {
        return "Contribution";
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