// package com.tourminators.backend;

// import java.util.*;
// import java.time.LocalTime;

// import backend.models.Node;
// import backend.models.Pair;
// import backend.models.Edge;
// import backend.TSP.Graph;
// import backend.models.PointOfInterest;
// import backend.models.PointOfInterest.PoIEnum;

// import org.junit.Assert;
// import org.junit.Before;
// import org.junit.Test;

// import ch.qos.logback.core.joran.spi.NoAutoStartUtil;

// public class GraphTest {

//     @Before
//     public void setup() {
//         // --- Création des sommets (nœuds) ---
//         Map<Long, Node> nodes = new HashMap<>();
//         Node n1 = new Node(1, 0, 0);
//         Node n2 = new Node(2, 3, 4);
//         Node n3 = new Node(3, 1, 2);
//         Node n4 = new Node(4, 8, 7);
//         Node n5 = new Node(5, 2, 8);
//         Node n6 = new Node(6, 5, 5);
//         Node n7 = new Node(7, 6, 5);

//         nodes.put(1L, n1);
//         nodes.put(2L, n2);
//         nodes.put(3L, n3);
//         nodes.put(4L, n4);
//         nodes.put(5L, n5);
//         nodes.put(6L, n6);
//         nodes.put(7L, n7);

//         // --- Création des arêtes (edges) ---
//         List<Edge> edges = new ArrayList<>();
//         edges.add(new Edge(1L, 2L, 5.0f, "node1-node2"));
//         edges.add(new Edge(2L, 3L, 2.0f, "node2-node3"));
//         edges.add(new Edge(1L, 3L, 1.0f, "node1-node3"));
//         edges.add(new Edge(3L, 2L, 1.0f, "node3-node4"));
//         edges.add(new Edge(3L, 4L, 4.0f, "node3-node4"));
//         edges.add(new Edge(2L, 5L, 3.0f, "node2-node5"));
//         edges.add(new Edge(5L, 4L, 7.0f, "node5-node4"));
//         edges.add(new Edge(4L, 6L, 2.0f, "node4-node6"));
//         edges.add(new Edge(5L, 6L, 4.0f, "node5-node6"));
//         edges.add(new Edge(6L, 5L, 4.0f, "node6-node5"));


//         // --- Points d’intérêt (tour) ---
//         Map<Long, PointOfInterest> tour = new HashMap<>();
//         tour.put(1L, new PointOfInterest(n1, PoIEnum.WAREHOUSE, null, 0));
//         tour.put(2L, new PointOfInterest(n2, PoIEnum.PICKUP, 6L, 10));
//         tour.put(6L, new PointOfInterest(n6, PoIEnum.DELIVERY, 2L, 5));

//         // --- Création du graphe ---
//         Graph graph = new Graph(nodes, edges, tour);

//         testInitialisation(graph);

//         testGetNodesToVisit(graph);

//         testGetAssociatedPoI(graph, 2L);
//         testGetAssociatedPoI(graph, 4L);

//         testGetBeginId(graph);

//         testGetNbNodes(graph);

//         testGetCost(graph,1L,2L);

//         testGetNeighbors(graph, 2L);
//         testGetNeighbors(graph, 6L);

//         testAWAStar(graph, 1L, 2L);
//         testAWAStar(graph, 1L, 6L);
//         testAWAStar(graph, 1L, 7L); // no path
//         testAWAStar(graph, 1L, 8L); // invalid id

//     }

//     @Test
//     public void testInitialisation(){
//         // --- Tests d’initialisation ---
//         Assert.assertEquals(7, graph.getAllNodes().size());
//         Assert.assertEquals(10, graph.getAllEdges().size());
//         Assert.assertEquals(7, graph.getAdjacency().size());
//         Assert.assertEquals(10, graph.getAllCosts().size());
//         Assert.assertTrue(graph.getPathCostMap().isEmpty());
//     }

//     @Test
//     public void testGetNodesToVisit(){
//         // --- Test getNodesToVisit ---
//         List<Long> nodesToVisit = graph.getNodesToVisit();
//         Assert.assertEquals(1, nodesToVisit.size());
//         Assert.assertTrue(nodesToVisit.contains(2L));
//     }

//     @Test
//     public void testGetAssociatedPoI(Graph graph, Long id){
//         // --- Test getAssociatedPoI ---
//         Long associatedPoI = graph.getAssociatedPoI(id);

//         if(id == 2L){
//             Assert.assertEquals(Long.valueOf(3L), associatedPoI);
//         } else if (id == 4L){
//             Assert.assertNull(associatedPoI);
//         }

//     }
//     @Test
//     public void testGetBeginId(Graph graph){
//         // --- Test getBeginId ---
//         Long beginId = graph.getBeginId();
//         Assert.assertEquals(Long.valueOf(1L), beginId);
//     }
//     @Test
//     public void testGetNbNodes(Graph graph){
//         // --- Test getNbNodes ---
//         int nbNodes = graph.getNbNodes();

//         Assert.assertEquals(7, nbNodes);
//     }
//     @Test
//     public void testGetCost(Graph graph, Long from, Long to){
//         // --- Test getCost ---
//         Float cost = graph.getCost(from, to);
//         Assert.assertNotNull(cost);
//         if(from == 1L && to == 2L){
//             Assert.assertEquals(Float.valueOf(5.0f), cost);
//         }
//     }
//     @Test
//     public void testGetNeighbors(Graph graph, Long nodeId){
//         // --- Test getNeighbors ---
//         Set<Long> neighbors = graph.getNeighbors(nodeId);
//         if(nodeId == 2L){
//             Assert.assertEquals(new HashSet<>(Arrays.asList(1L,3L,5L)), neighbors);
//         } else if (nodeId == 6L){
//             Assert.assertEquals(new HashSet<>(Arrays.asList(5L)), neighbors);
//         }
//     }
//     @Test
//     public void testAWAStar(Graph graph, Long startId, Long endId){
//         // --- Test AWA* ---
//         //graph.AWAStar(startId, endId);
//         Map<Long,Long> pathCost = graph.AWAStar(startId, endId);
        
//         if(startId == 1L && endId == 2L){
//             Assert.assertNotNull(pathCost);
//             Assert.assertTrue(pathCost.containsKey(endId));
//             Assert.assertEquals(Long.valueOf(5L), pathCost.get(endId));
//         } else if (startId == 1L && endId == 6L){
//             Assert.assertNotNull(pathCost);
//             Assert.assertTrue(pathCost.containsKey(endId));
//             Assert.assertEquals(Long.valueOf(11L), pathCost.get(endId));
//         } else if (startId == 1L && endId == 7L){
//             Assert.assertNotNull(pathCost);
//             Assert.assertFalse(pathCost.containsKey(endId)); // No path should exist
//         } else if (startId == 1L && endId == 8L){
//             Assert.assertNull(pathCost); // Invalid ID should return null
//         }

//     }
//     @Test
//     public void testGetPathCost(Graph graph){
//         System.out.println("\n=== TEST getPathCost ===");
//         for(Node n : graph.getAllNodes().values()){
//             for(Node m : graph.getAllNodes().values()){
//                 Float cost = graph.getPathCost(n.getId(), m.getId());
//                 if(cost != null){
//                     System.out.println("Path cost from " + n.getId() + " to " + m.getId() + ": " + cost);
//                 }
//             }
//         }
//         if(graph.getPathCostMap().containsKey(new Pair<Long, Long>(1L, 2L))){
//             Assert.assertEquals(Float.valueOf(5.0f), graph.getPathCost(1L, 2L));
//         }
//     }
// }
