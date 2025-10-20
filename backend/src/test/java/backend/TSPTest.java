package backend;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;

import backend.TSP.TSP2;
import backend.models.Edge;
import backend.models.Node;
import backend.models.Pair;
import backend.models.PointOfInterest;
import backend.TSP.Graph;


public class TSPTest {
   @Test
   public void testTSP() {
        Map<Long, Node> nodes = new HashMap<>();
        nodes.put(0L, new Node(0, 45.751904, 4.857877));  // warehouse
        nodes.put(1L, new Node(1, 45.752000, 4.860000));  // delivery
        nodes.put(2L, new Node(2, 45.753000, 4.861000));  // pickup


        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge(0L, 1L, 250.0f, "edge_0_1"));

        edges.add(new Edge(1L, 2L, 300.0f, "edge_1_2"));
        edges.add(new Edge(2L, 1L, 300.0f, "edge_2_1"));
        
        edges.add(new Edge(2L, 0L, 350.0f, "edge_2_0"));


        List<PointOfInterest> tour = new ArrayList<>();
        tour.add(new PointOfInterest(nodes.get(0L), PointOfInterest.PoIEnum.WAREHOUSE, null, 0));
        tour.add(new PointOfInterest(nodes.get(1L), PointOfInterest.PoIEnum.DELIVERY, 2L, 200));
        tour.add(new PointOfInterest(nodes.get(2L), PointOfInterest.PoIEnum.PICKUP, 1L, 300));

        // Order is: warehouse (0) -> pickup (2) -> delivery (1) -> warehouse (0)
        // Full path: 0 -> 1 -> 2 -> 1 -> 2 -> 0
        // Total cost:  250 + 300 + 300 + 300 + 350 = 1500.0

		Graph g = new Graph(nodes, edges, tour);
        TSP2 tsp = new TSP2(60000, g); // 60 seconds time limit
		tsp.chercheSolution();

        LinkedList<Long> expectedOrder = new LinkedList<>();
        expectedOrder.add(0L);
        expectedOrder.add(1L);
        expectedOrder.add(2L);
        expectedOrder.add(1L);
        expectedOrder.add(2L);
        expectedOrder.add(0L);

        Assert.assertEquals(expectedOrder, tsp.getSolutionOrder());

        double expectedCost = 1500.0;
        Assert.assertEquals(expectedCost, tsp.getCoutSolution(), 0.001);

        LinkedHashSet<Map<Pair<Long, Long>, LinkedList<Long>>> expectedPath = new LinkedHashSet<>();
        LinkedList<Long> path0to2 = new LinkedList<>();
        path0to2.add(0L);
        path0to2.add(1L);
        path0to2.add(2L);
        expectedPath.add(Map.of(new Pair<>(0L, 2L), path0to2));
        LinkedList<Long> path2to1 = new LinkedList<>();
        path2to1.add(2L);
        path2to1.add(1L);
        expectedPath.add(Map.of(new Pair<>(2L, 1L), path2to1));
        LinkedList<Long> path1to0 = new LinkedList<>();
        path1to0.add(1L);
        path1to0.add(2L);
        path1to0.add(0L);
        expectedPath.add(Map.of(new Pair<>(1L, 0L), path1to0));

        Assert.assertEquals(expectedPath, tsp.getSolutionPath());
    }
}
