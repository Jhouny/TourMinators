public class Features {

    public static list<Node> someNodes() {
        sommets = new list<Node>();
        sommets.add(new Node(1, 45, 4));
        sommets.add(new Node(2, 46, 5));
        sommets.add(new Node(3, 43, 0));
        return sommets;
    }

    public static list<Edge> someEdges() {
        edges = new ArrayList<Edge>();
        edges.add(new Edge(1, 2, "a"));
        edges.add(new Edge(2, 3, "b"));
        edges.add(new Edge(3, 1, "c"));
        edges.add(new Edge(3, 2, "c"));
        return edges;
    }
}
