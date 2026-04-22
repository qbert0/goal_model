package org.vnu.sme.goal.view;

import java.util.HashSet;
import java.util.Set;

import org.tzi.use.gui.views.diagrams.DiagramView.DiagramData;
import org.tzi.use.gui.views.diagrams.elements.PlaceableNode;
import org.tzi.use.gui.views.diagrams.elements.edges.EdgeBase;

public class GoalDiagramData implements DiagramData {
    private final Set<PlaceableNode> nodes = new HashSet<>();
    private final Set<EdgeBase> edges = new HashSet<>();

    public void addNode(PlaceableNode node) {
        nodes.add(node);
    }

    public void addEdge(EdgeBase edge) {
        edges.add(edge);
    }

    public void clear() {
        nodes.clear();
        edges.clear();
    }

    @Override
    public Set<PlaceableNode> getNodes() {
        return nodes;
    }

    @Override
    public Set<EdgeBase> getEdges() {
        return edges;
    }

    @Override
    public boolean hasNodes() {
        return !nodes.isEmpty();
    }
}
