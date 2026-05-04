package org.vnu.sme.goal.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import org.tzi.use.gui.views.View;
import org.vnu.sme.goal.mm.bpmn.BpmnCollaboration;
import org.vnu.sme.goal.mm.bpmn.BpmnEndEvent;
import org.vnu.sme.goal.mm.bpmn.BpmnExclusiveGateway;
import org.vnu.sme.goal.mm.bpmn.BpmnFlowNode;
import org.vnu.sme.goal.mm.bpmn.BpmnMessageFlow;
import org.vnu.sme.goal.mm.bpmn.BpmnModel;
import org.vnu.sme.goal.mm.bpmn.BpmnParallelGateway;
import org.vnu.sme.goal.mm.bpmn.BpmnParticipant;
import org.vnu.sme.goal.mm.bpmn.BpmnProcess;
import org.vnu.sme.goal.mm.bpmn.BpmnSequenceFlow;
import org.vnu.sme.goal.mm.bpmn.BpmnStartEvent;
import org.vnu.sme.goal.mm.bpmn.BpmnTask;

public final class BpmnDiagramView extends JPanel implements View {
    private static final int LEFT_MARGIN = 40;
    private static final int TOP_MARGIN = 40;
    private static final int POOL_HEADER_WIDTH = 160;
    private static final int POOL_HEIGHT = 170;
    private static final int POOL_GAP = 48;
    private static final int COLUMN_GAP = 170;
    private static final int NODE_GAP = 95;
    private static final int TASK_WIDTH = 125;
    private static final int TASK_HEIGHT = 60;
    private static final int EVENT_SIZE = 56;
    private static final int GATEWAY_SIZE = 64;

    private final BpmnModel bpmnModel;
    private final Map<BpmnFlowNode, Rectangle> boundsByNode = new HashMap<>();
    private final Map<BpmnParticipant, Rectangle> poolBounds = new LinkedHashMap<>();

    public BpmnDiagramView(BpmnModel bpmnModel) {
        this.bpmnModel = bpmnModel;
        setBackground(Color.WHITE);
        setOpaque(true);
        ToolTipManager.sharedInstance().registerComponent(this);
        layoutDiagram();
    }

    @Override
    public void detachModel() {
        // Static visualization only.
    }

    @Override
    public String getToolTipText(MouseEvent event) {
        for (Map.Entry<BpmnFlowNode, Rectangle> entry : boundsByNode.entrySet()) {
            if (entry.getValue().contains(event.getPoint())) {
                BpmnFlowNode node = entry.getKey();
                return "<html><b>" + escape(node.getName()) + "</b><br/>"
                        + escape(node.getClass().getSimpleName()) + "</html>";
            }
        }
        for (Map.Entry<BpmnParticipant, Rectangle> entry : poolBounds.entrySet()) {
            if (entry.getValue().contains(event.getPoint())) {
                BpmnParticipant participant = entry.getKey();
                return "<html><b>Pool: " + escape(participant.getName()) + "</b><br/>process: "
                        + escape(participant.getProcess().getName()) + "</html>";
            }
        }
        return null;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setColor(Color.BLACK);
        g2d.drawString("BPMN Model: " + bpmnModel.getName(), 16, 24);

        paintPools(g2d);
        paintSequenceFlows(g2d);
        paintMessageFlows(g2d);
        paintNodes(g2d);
        g2d.dispose();
    }

    private void paintPools(Graphics2D g2d) {
        BpmnCollaboration collaboration = bpmnModel.getCollaboration();
        if (collaboration == null) {
            return;
        }

        g2d.setColor(new Color(55, 71, 79));
        g2d.drawString("Collaboration: " + collaboration.getName(), LEFT_MARGIN, TOP_MARGIN - 12);
        for (Map.Entry<BpmnParticipant, Rectangle> entry : poolBounds.entrySet()) {
            Rectangle pool = entry.getValue();
            BpmnParticipant participant = entry.getKey();

            g2d.setColor(new Color(245, 247, 250));
            g2d.fillRoundRect(pool.x, pool.y, pool.width, pool.height, 18, 18);
            g2d.setColor(new Color(150, 160, 170));
            g2d.drawRoundRect(pool.x, pool.y, pool.width, pool.height, 18, 18);

            g2d.setColor(new Color(230, 235, 240));
            g2d.fillRoundRect(pool.x, pool.y, POOL_HEADER_WIDTH, pool.height, 18, 18);
            g2d.setColor(new Color(150, 160, 170));
            g2d.drawLine(pool.x + POOL_HEADER_WIDTH, pool.y, pool.x + POOL_HEADER_WIDTH, pool.y + pool.height);

            g2d.setColor(new Color(33, 33, 33));
            g2d.drawString("Pool", pool.x + 16, pool.y + 24);
            g2d.drawString(participant.getName(), pool.x + 16, pool.y + 48);
            g2d.drawString("Process", pool.x + 16, pool.y + 82);
            g2d.drawString(participant.getProcess().getName(), pool.x + 16, pool.y + 106);
        }
    }

    private void paintSequenceFlows(Graphics2D g2d) {
        for (BpmnParticipant participant : poolBounds.keySet()) {
            for (BpmnSequenceFlow flow : participant.getProcess().getSequenceFlows()) {
                Rectangle source = boundsByNode.get(flow.getSource());
                Rectangle target = boundsByNode.get(flow.getTarget());
                if (source == null || target == null) {
                    continue;
                }
                int x1 = source.x + source.width;
                int y1 = source.y + source.height / 2;
                int x2 = target.x;
                int y2 = target.y + target.height / 2;
                int midX = (x1 + x2) / 2;

                g2d.setColor(new Color(80, 80, 80));
                g2d.setStroke(new BasicStroke(1.5f));
                g2d.drawLine(x1, y1, midX, y1);
                g2d.drawLine(midX, y1, midX, y2);
                g2d.drawLine(midX, y2, x2, y2);
                drawArrowHead(g2d, x2, y2, new Color(80, 80, 80));
            }
        }
    }

    private void paintMessageFlows(Graphics2D g2d) {
        BpmnCollaboration collaboration = bpmnModel.getCollaboration();
        if (collaboration == null) {
            return;
        }
        float[] dash = new float[] { 6f, 6f };
        g2d.setStroke(new BasicStroke(1.4f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, dash, 0f));
        for (BpmnMessageFlow flow : collaboration.getMessageFlows()) {
            Rectangle source = boundsByNode.get(flow.getSourceNode());
            Rectangle target = boundsByNode.get(flow.getTargetNode());
            if (source == null || target == null) {
                continue;
            }

            int x1 = source.x + source.width / 2;
            int y1 = source.y + source.height;
            int x2 = target.x + target.width / 2;
            int y2 = target.y;
            int midY = (y1 + y2) / 2;

            g2d.setColor(new Color(0, 121, 107));
            g2d.drawLine(x1, y1, x1, midY);
            g2d.drawLine(x1, midY, x2, midY);
            g2d.drawLine(x2, midY, x2, y2);
            drawArrowHead(g2d, x2, y2, new Color(0, 121, 107));

            FontMetrics fm = g2d.getFontMetrics();
            int labelWidth = fm.stringWidth(flow.getName());
            g2d.drawString(flow.getName(), (x1 + x2 - labelWidth) / 2, midY - 6);
        }
        g2d.setStroke(new BasicStroke(1f));
    }

    private void paintNodes(Graphics2D g2d) {
        for (BpmnParticipant participant : poolBounds.keySet()) {
            for (BpmnFlowNode node : participant.getProcess().getFlowNodes()) {
                Rectangle bounds = boundsByNode.get(node);
                if (bounds == null) {
                    continue;
                }
                if (node instanceof BpmnStartEvent) {
                    paintStartEvent(g2d, node, bounds);
                } else if (node instanceof BpmnEndEvent) {
                    paintEndEvent(g2d, node, bounds);
                } else if (node instanceof BpmnTask) {
                    paintTask(g2d, node, bounds);
                } else if (node instanceof BpmnExclusiveGateway) {
                    paintGateway(g2d, node, bounds, "X");
                } else if (node instanceof BpmnParallelGateway) {
                    paintGateway(g2d, node, bounds, "+");
                }
            }
        }
    }

    private void paintStartEvent(Graphics2D g2d, BpmnFlowNode node, Rectangle bounds) {
        g2d.setColor(new Color(220, 245, 223));
        g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
        g2d.setColor(new Color(56, 142, 60));
        g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
        drawCenteredLabel(g2d, node.getName(), bounds, EVENT_SIZE + 16);
    }

    private void paintEndEvent(Graphics2D g2d, BpmnFlowNode node, Rectangle bounds) {
        g2d.setColor(new Color(255, 230, 230));
        g2d.fillOval(bounds.x, bounds.y, bounds.width, bounds.height);
        g2d.setColor(new Color(183, 28, 28));
        g2d.setStroke(new BasicStroke(2f));
        g2d.drawOval(bounds.x, bounds.y, bounds.width, bounds.height);
        g2d.drawOval(bounds.x + 4, bounds.y + 4, bounds.width - 8, bounds.height - 8);
        g2d.setStroke(new BasicStroke(1f));
        drawCenteredLabel(g2d, node.getName(), bounds, EVENT_SIZE + 16);
    }

    private void paintTask(Graphics2D g2d, BpmnFlowNode node, Rectangle bounds) {
        g2d.setColor(new Color(225, 239, 255));
        g2d.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 18, 18);
        g2d.setColor(new Color(25, 118, 210));
        g2d.setStroke(new BasicStroke(1.8f));
        g2d.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 18, 18);
        g2d.setStroke(new BasicStroke(1f));
        drawCenteredText(g2d, node.getName(), bounds);
    }

    private void paintGateway(Graphics2D g2d, BpmnFlowNode node, Rectangle bounds, String marker) {
        int cx = bounds.x + bounds.width / 2;
        int cy = bounds.y + bounds.height / 2;
        Polygon diamond = new Polygon(
                new int[] { cx, bounds.x + bounds.width, cx, bounds.x },
                new int[] { bounds.y, cy, bounds.y + bounds.height, cy },
                4);
        g2d.setColor(new Color(255, 249, 196));
        g2d.fillPolygon(diamond);
        g2d.setColor(new Color(123, 98, 0));
        g2d.drawPolygon(diamond);

        FontMetrics fm = g2d.getFontMetrics();
        int markerWidth = fm.stringWidth(marker);
        g2d.drawString(marker, cx - markerWidth / 2, cy + fm.getAscent() / 2 - 2);
        drawCenteredLabel(g2d, node.getName(), bounds, GATEWAY_SIZE + 18);
    }

    private void drawCenteredText(Graphics2D g2d, String text, Rectangle bounds) {
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = bounds.x + (bounds.width - textWidth) / 2;
        int y = bounds.y + bounds.height / 2 + fm.getAscent() / 2 - 2;
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x, y);
    }

    private void drawCenteredLabel(Graphics2D g2d, String text, Rectangle bounds, int dy) {
        FontMetrics fm = g2d.getFontMetrics();
        int textWidth = fm.stringWidth(text);
        int x = bounds.x + (bounds.width - textWidth) / 2;
        int y = bounds.y + dy;
        g2d.setColor(Color.BLACK);
        g2d.drawString(text, x, y);
    }

    private void drawArrowHead(Graphics2D g2d, int x, int y, Color color) {
        g2d.setColor(color);
        Polygon arrowHead = new Polygon(
                new int[] { x, x - 10, x - 10 },
                new int[] { y, y - 5, y + 5 },
                3);
        g2d.fillPolygon(arrowHead);
    }

    private void layoutDiagram() {
        boundsByNode.clear();
        poolBounds.clear();

        int currentTop = TOP_MARGIN;
        int maxRight = 1100;
        int maxBottom = 600;
        BpmnCollaboration collaboration = bpmnModel.getCollaboration();
        if (collaboration == null) {
            setPreferredSize(new Dimension(maxRight, maxBottom));
            return;
        }

        for (BpmnParticipant participant : collaboration.getParticipants()) {
            int poolTop = currentTop;
            int poolWidth = layoutParticipant(participant, poolTop);
            Rectangle poolBoundsRect = new Rectangle(LEFT_MARGIN, poolTop, poolWidth, POOL_HEIGHT);
            poolBounds.put(participant, poolBoundsRect);
            maxRight = Math.max(maxRight, poolBoundsRect.x + poolBoundsRect.width + 40);
            maxBottom = Math.max(maxBottom, poolBoundsRect.y + poolBoundsRect.height + 40);
            currentTop += POOL_HEIGHT + POOL_GAP;
        }

        setPreferredSize(new Dimension(maxRight, maxBottom));
        revalidate();
        repaint();
    }

    private int layoutParticipant(BpmnParticipant participant, int poolTop) {
        Map<BpmnFlowNode, Integer> levels = computeLevels(participant.getProcess());
        Map<Integer, List<BpmnFlowNode>> byLevel = new LinkedHashMap<>();
        for (BpmnFlowNode node : participant.getProcess().getFlowNodes()) {
            byLevel.computeIfAbsent(levels.getOrDefault(node, 0), ignored -> new ArrayList<>()).add(node);
        }
        for (List<BpmnFlowNode> nodes : byLevel.values()) {
            nodes.sort(Comparator.comparing(BpmnFlowNode::getName));
        }

        int maxRight = LEFT_MARGIN + POOL_HEADER_WIDTH + 80;
        for (Map.Entry<Integer, List<BpmnFlowNode>> entry : byLevel.entrySet()) {
            int level = entry.getKey();
            int x = LEFT_MARGIN + POOL_HEADER_WIDTH + 40 + level * COLUMN_GAP;
            List<BpmnFlowNode> columnNodes = entry.getValue();
            for (int i = 0; i < columnNodes.size(); i++) {
                BpmnFlowNode node = columnNodes.get(i);
                int y = poolTop + 28 + i * NODE_GAP;
                Rectangle bounds = new Rectangle(x, y, nodeWidth(node), nodeHeight(node));
                boundsByNode.put(node, bounds);
                maxRight = Math.max(maxRight, bounds.x + bounds.width + 40);
            }
        }
        return maxRight - LEFT_MARGIN;
    }

    private Map<BpmnFlowNode, Integer> computeLevels(BpmnProcess process) {
        Map<BpmnFlowNode, Integer> levels = new HashMap<>();
        Deque<BpmnFlowNode> work = new ArrayDeque<>(process.getFlowNodesByType(BpmnStartEvent.class));
        if (work.isEmpty()) {
            work.addAll(process.getFlowNodes());
        }

        while (!work.isEmpty()) {
            BpmnFlowNode node = work.removeFirst();
            int level = levels.getOrDefault(node, 0);
            for (BpmnFlowNode next : process.getOutgoing(node)) {
                int nextLevel = Math.max(levels.getOrDefault(next, 0), level + 1);
                if (nextLevel != levels.getOrDefault(next, 0)) {
                    levels.put(next, nextLevel);
                }
                work.addLast(next);
            }
        }

        int fallbackLevel = levels.values().stream().mapToInt(Integer::intValue).max().orElse(0) + 1;
        for (BpmnFlowNode node : process.getFlowNodes()) {
            levels.putIfAbsent(node, fallbackLevel);
        }
        return levels;
    }

    private int nodeWidth(BpmnFlowNode node) {
        if (node instanceof BpmnTask) {
            return TASK_WIDTH;
        }
        if (node instanceof BpmnExclusiveGateway || node instanceof BpmnParallelGateway) {
            return GATEWAY_SIZE;
        }
        return EVENT_SIZE;
    }

    private int nodeHeight(BpmnFlowNode node) {
        if (node instanceof BpmnTask) {
            return TASK_HEIGHT;
        }
        if (node instanceof BpmnExclusiveGateway || node instanceof BpmnParallelGateway) {
            return GATEWAY_SIZE;
        }
        return EVENT_SIZE;
    }

    private String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
