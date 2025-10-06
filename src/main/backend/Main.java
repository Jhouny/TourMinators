package backend;

import backend.models.Node;
import backend.models.Edge;
import java.util.Map;
import java.util.List;

public class Main {
    public static void main(String[] args) throws Exception {
        String xmlPath = "src/ressources/petitPlan.xml";
        Map<Long, Node> nodes = XMLParser.parseNodes(xmlPath);
        List<Edge> edges = XMLParser.parseEdges(xmlPath);
        System.out.println("Nodes: " + nodes.size());
        System.out.println("Edges: " + edges.size());
    }
}
