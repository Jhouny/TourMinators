package backend.TSP;

import java.util.*;
import java.time.LocalTime;

import backend.models.Node;
import backend.models.Pair;
import backend.models.Edge;
import backend.models.PointOfInterest;
import backend.models.PointOfInterest.PoIEnum;

public class TestGraph {

    public static void main(String[] args) {
        // --- Création des sommets (nœuds) ---
        Map<Long, Node> nodes = new HashMap<>();
        Node n1 = new Node(1, 0, 0);
        Node n2 = new Node(2, 3, 4);
        Node n3 = new Node(3, 1, 2);
        Node n4 = new Node(4, 8, 7);
        Node n5 = new Node(5, 2, 8);
        Node n6 = new Node(6, 5, 5);
        Node n7 = new Node(7, 6, 5);

        nodes.put(1L, n1);
        nodes.put(2L, n2);
        nodes.put(3L, n3);
        nodes.put(4L, n4);
        nodes.put(5L, n5);
        nodes.put(6L, n6);
        nodes.put(7L, n7);

        // --- Création des arêtes (edges) ---
        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge(1L, 2L, 5.0f, "node1-node2"));
        edges.add(new Edge(2L, 3L, 2.0f, "node2-node3"));
        edges.add(new Edge(1L, 3L, 1.0f, "node1-node3"));
        edges.add(new Edge(3L, 2L, 1.0f, "node3-node4"));
        edges.add(new Edge(3L, 4L, 4.0f, "node3-node4"));
        edges.add(new Edge(2L, 5L, 3.0f, "node2-node5"));
        edges.add(new Edge(5L, 4L, 7.0f, "node5-node4"));
        edges.add(new Edge(4L, 6L, 2.0f, "node4-node6"));
        edges.add(new Edge(5L, 6L, 4.0f, "node5-node6"));
        edges.add(new Edge(6L, 5L, 4.0f, "node6-node5"));


        // --- Points d’intérêt (tour) ---
        Map<Long, PointOfInterest> tour = new HashMap<>();
        tour.put(1L, new PointOfInterest(n1, PoIEnum.WAREHOUSE, null, 0));
        tour.put(2L, new PointOfInterest(n2, PoIEnum.PICKUP, 3L, 10));
        tour.put(6L, new PointOfInterest(n6, PoIEnum.DELIVERY, 2L, 5));

        // --- Création du graphe ---
        Graph graph = new Graph(nodes, edges, tour);

        // testInitialisation(graph); //validated 
        // testGetNodesToVisit(graph); //validated
        // testGetAssociatedPoI(graph, 2L); //validated
        //testGetAssociatedPoI(graph, 4L); //null, validated
        
        // testGetBeginId(graph); //validated
        // testGetNbNodes(graph); //validated

        // testGetCost(graph,1L,2L); //validated
        // //testGetCost(graph,2L,4L); //suppose to throw exception (no edge), validated
        
        // testGetPathCost(graph); //validated

        // testGetNeighbors(graph, 2L); //validated
        // testGetNeighbors(graph, 6L); //validated

        // IMPORTANT ! Pour les tests AWA*, il faut que les latitudes et longitudes soient réalistes
        // Puisqu'elles sont utilisées pour les heuristiques

        testAWAStar(graph, 1L, 2L); //validated 
        testAWAStar(graph, 1L, 6L);
        testAWAStar(graph, 1L,7L); //tester pour un noeud qui est dans le graph mais pas connecté
        testAWAStar(graph, 1L,8L); //tester pour un ID invalide

        System.out.println("\n=== PATH COST ===");
        System.out.println("Path cost from 1 to 2: " + graph.getPathCost(1L, 2L));
        System.out.println("Path cost from 1 to 6: " + graph.getPathCost(1L, 6L));
        System.out.println("Path cost from 1 to 7: " + graph.getPathCost(1L, 7L)); // should be
        System.out.println("Path cost from 1 to 8: " + graph.getPathCost(1L, 8L)); // should be
        System.out.println();
    }

    public static void testInitialisation(Graph graph){
        // --- Tests d’initialisation ---
        System.out.println("=== TEST INITIALISATION GRAPH ===");
        System.out.println("Nodes: " + graph.all_nodes.keySet());
        System.out.println("Edges:");
        for (Edge e : graph.all_edges) {
            System.out.println("  " + e.getOrigin() + " -> " + e.getDestination() + " (" + e.getLength() + ")");
        }

        System.out.println("\nAdjacency:");
        for (Map.Entry<Long, Set<Long>> entry : graph.adjacency.entrySet()) {
            System.out.println("  " + entry.getKey() + " -> " + entry.getValue());
        }

        System.out.println("\nAll costs:");
        for (Map.Entry<Pair<Long, Long>, Float> entry : graph.all_costs.entrySet()) {
            System.out.println("  " + entry.getKey() + " : " + entry.getValue());
        }

        System.out.println("\nPath cost (devrait être vide au début): " + graph.pathCost);
    }

    public static void testGetNodesToVisit(Graph graph){
        // --- Test getNodesToVisit ---
        System.out.println("\n=== TEST getNodesToVisit ===");
        List<Long> nodesToVisit = graph.getNodesToVisit();
        System.out.println("Nodes to visit (pickups only): " + nodesToVisit);
    }

    public static void testGetAssociatedPoI(Graph graph, Long id){
        // --- Test getAssociatedPoI ---
        System.out.println("\n=== TEST getAssociatedPoI ===");
        Long associatedPoI = graph.getAssociatedPoI(id);
        System.out.println("Associated PoI for node " + id + ": " + associatedPoI);
    }

    public static void testGetBeginId(Graph graph){
        // --- Test getBeginId ---
        System.out.println("\n=== TEST getBeginId ===");
        Long beginId = graph.getBeginId();
        System.out.println("Begin ID (warehouse): " + beginId);
    }

    public static void testGetNbNodes(Graph graph){
        // --- Test getNbNodes ---
        System.out.println("\n=== TEST getNbNodes ===");
        int nbNodes = graph.getNbNodes();
        System.out.println("Number of nodes: " + nbNodes);
    }

    public static void testGetCost(Graph graph, Long from, Long to){
        // --- Test getCost ---
        System.out.println("\n=== TEST getCost ===");
        Float cost = graph.getCost(from, to);
        System.out.println("Cost from " + from + " to " + to + ": " + cost);
    }
    public static void testGetPathCost(Graph graph){
        // --- Test getPathCost ---
        System.out.println("\n=== TEST getPathCost ===");
        Float pathCost = graph.getPathCost(Long.valueOf(1), Long.valueOf(2));
        System.out.println("Path cost (should be empty initially): " + pathCost);
    }
    public static void testGetNeighbors(Graph graph, Long nodeId){
        // --- Test getNeighbors ---
        System.out.println("\n=== TEST getNeighbors ===");
        Set<Long> neighbors = graph.getNeighbors(nodeId);
        System.out.println("Neighbors of node " + nodeId + ": " + neighbors);
    }
    public static void testAWAStar(Graph graph, Long startId, Long endId){
        // --- Test AWA* ---
        System.out.println("\n=== TEST AWA* ===");
        //graph.AWAStar(startId, endId);
        Map<Long,Long> pathCost = graph.AWAStar(startId, endId);
        System.out.println("Path cost from " + startId + " to " + endId + " after AWA*: " + pathCost);
    }

}
