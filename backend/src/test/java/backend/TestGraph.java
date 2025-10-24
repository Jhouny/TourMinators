package backend;

import java.util.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.Assert;

import backend.models.Node;
import backend.TSP.Graph;
import backend.models.Edge;
import backend.models.PointOfInterest;
import backend.models.PointOfInterest.PoIEnum;

public class TestGraph {
    private Graph graph;
    private Map<Long, Node> nodes = new HashMap<>();
    private Map<Long, PointOfInterest> tour = new HashMap<>();
    private List<Edge> edges = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        // --- Création des sommets (nœuds) ---

        Node n1 = new Node(1, 0, 0);
        Node n2 = new Node(2, 2, 2);
        Node n3 = new Node(3, 0, 1);
        Node n4 = new Node(4, 0, 5);
        Node n5 = new Node(5, 4, 4);
        Node n6 = new Node(6, 2, 4);
        Node n7 = new Node(7, 6, 5);

        nodes.put(1L, n1);
        nodes.put(2L, n2);
        nodes.put(3L, n3);
        nodes.put(4L, n4);
        nodes.put(5L, n5);
        nodes.put(6L, n6);
        nodes.put(7L, n7);

        // --- Création des arêtes (edges) ---

        edges.add(new Edge(1L, 2L, 3.0f, "node1-node2"));
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

        tour.put(1L, new PointOfInterest(n1, PoIEnum.WAREHOUSE, null, 0));
        tour.put(2L, new PointOfInterest(n2, PoIEnum.PICKUP, 6L, 10));
        tour.put(6L, new PointOfInterest(n6, PoIEnum.DELIVERY, 2L, 5));

        // --- Création du graphe ---
        graph = new Graph(nodes, edges, tour);
    }

    @Test
    public void getPickupPoIs_ShouldReturnAllPickups() {

        LinkedHashSet<Long> expectedNodesToVisit = new LinkedHashSet<>();
        expectedNodesToVisit.add(2L); // pickup

        Assert.assertEquals(expectedNodesToVisit, graph.getPickupPoIs());
    }

    @Test
    public void getAssociatedPoI_ShouldReturnAssociatedPoi() {

        Long pickupId = 2L;
        Long deliveryId = 6L;

        Assert.assertEquals(pickupId, graph.getAssociatedPoI(deliveryId));
        Assert.assertEquals(deliveryId, graph.getAssociatedPoI(pickupId));
    }

    @Test
    public void getBeginId_ShouldReturnWarehouseId() {

        Long warehouseId = 1L;

        Assert.assertEquals(warehouseId, graph.getBeginId());
    }

    @Test
    public void getPathCost_ShouldRaiseErrorIfPoiAreNotConnected() {

        tour.put(7L, new PointOfInterest(nodes.get(7L), PoIEnum.PICKUP, 3L, 5));
        graph = new Graph(nodes, edges, tour);

        Assert.assertThrows(
                IllegalArgumentException.class,
                () -> {
                    graph.getPathCost(1L, 7L);
                });
    }

    // ############### Failing #####################
    // @Test
    // public void getPathCost_ShouldReturnPathCost(){

    // Assert.assertEquals(7.0, graph.getPathCost(1L, 6L), 0.001);
    // }

    // @Test
    // public void testAWAStar(){

    // Map<Long, Long> exptectedOptimalPath = new HashMap<>();
    // exptectedOptimalPath.put(6L, 4L);
    // exptectedOptimalPath.put(4L, 3L);
    // exptectedOptimalPath.put(3L, 1L);
    // exptectedOptimalPath.put(1L, null);
    // Assert.assertEquals(exptectedOptimalPath, graph.AWAStar(1L, 6L));
    // }

    // ############### Not implemented #####################

    // @Test
    // public void testInitialisation(Graph graph){
    // // // --- Tests d’initialisation ---
    // // System.out.println("=== TEST INITIALISATION GRAPH ===");
    // // System.out.println("Nodes: " + graph.all_nodes.keySet());
    // // System.out.println("Edges:");
    // // for (Edge e : graph.all_edges) {
    // // System.out.println(" " + e.getOrigin() + " -> " + e.getDestination() + "
    // (" + e.getLength() + ")");
    // // }

    // // System.out.println("\nAdjacency:");
    // // for (Map.Entry<Long, Set<Long>> entry : graph.adjacency.entrySet()) {
    // // System.out.println(" " + entry.getKey() + " -> " + entry.getValue());
    // // }

    // // System.out.println("\nAll costs:");
    // // for (Map.Entry<Pair<Long, Long>, Float> entry :
    // graph.all_costs.entrySet()) {
    // // System.out.println(" " + entry.getKey() + " : " + entry.getValue());
    // // }

    // // System.out.println("\nPath cost (devrait être vide au début): " +
    // graph.pathCost);
    // }

    // public static void testGetNbNodes(Graph graph){
    // // --- Test getNbNodes ---
    // System.out.println("\n=== TEST getNbNodes ===");
    // int nbNodes = graph.getNbNodes();
    // System.out.println("Number of nodes: " + nbNodes);
    // }

    // public static void testGetCost(Graph graph, Long from, Long to){
    // // --- Test getCost ---
    // System.out.println("\n=== TEST getCost ===");
    // Float cost = graph.getCost(from, to);
    // System.out.println("Cost from " + from + " to " + to + ": " + cost);
    // }

    // public static void testGetNeighbors(Graph graph, Long nodeId){
    // // --- Test getNeighbors ---
    // System.out.println("\n=== TEST getNeighbors ===");
    // Set<Long> neighbors = graph.getNeighbors(nodeId);
    // System.out.println("Neighbors of node " + nodeId + ": " + neighbors);
    // }

}
