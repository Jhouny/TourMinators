package backend;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.time.LocalTime;
import java.util.ArrayList;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

import backend.TSP.TSP;
import backend.TSP.TSP2;
import backend.models.Edge;
import backend.models.Node;
import backend.models.Pair;
import backend.models.PointOfInterest;
import backend.TSP.Graph;

public class TestTSP {
    private TSP tsp;
    private Graph graph;
    private Map<Long, Node> nodes = new HashMap<>();
    private Map<Long, PointOfInterest> tour = new HashMap<>();
    private List<Edge> edges = new ArrayList<>();

    @Before
    public void setUp() {

        nodes.put(0L, new Node(0, 45.751904, 4.857877));
        nodes.put(1L, new Node(1, 45.752000, 4.860000));
        nodes.put(2L, new Node(2, 45.753000, 4.861000));

        edges.add(new Edge(0L, 1L, 250.0f, "edge_0_1"));
        edges.add(new Edge(1L, 2L, 300.0f, "edge_1_2"));
        edges.add(new Edge(2L, 1L, 300.0f, "edge_2_1"));
        edges.add(new Edge(2L, 0L, 350.0f, "edge_2_0"));

        tour.put(0L, new PointOfInterest(nodes.get(0L), PointOfInterest.PoIEnum.WAREHOUSE, null, 0));
        tour.put(1L, new PointOfInterest(nodes.get(1L), PointOfInterest.PoIEnum.DELIVERY, 2L, 200));
        tour.put(2L, new PointOfInterest(nodes.get(2L), PointOfInterest.PoIEnum.PICKUP, 1L, 300));

        graph = new Graph(nodes, edges, tour);
        tsp = new TSP2(60000, graph); // 60 seconds time limit
    }

    @Test
    public void chercheSolution_ShouldInstaciateSolutionsDataInGraph() {
        tsp.chercheSolution();
        Assert.assertEquals(tsp.getSolutionOrder().size(), tour.size() + 1); // +1 for returning to warehouse
        Assert.assertEquals(tsp.getSolutionPath().size(), tour.size());
    }

    @Test
    public void getSolutionOrder_ShouldGiveSolutionOrderAndArrivalTimes() {
        tsp.chercheSolution();

        // Order is: warehouse (0) -> pickup (2) -> delivery (1) -> warehouse (0)
        // Full path: 0 -> 1 -> 2 -> 1 -> 2 -> 0
        // costs: 250 + 300 + 300 + 300 + 350

        LocalTime time = LocalTime.of(8, 0); // 8:00 AM start time
        LinkedList<Pair<Long, LocalTime>> expectedOrder = new LinkedList<>();
        expectedOrder.add(new Pair<Long, LocalTime>(0L, time));
        time = time.plusSeconds((long) (3600.0 * (250.0 + 300.0) / 15000.0)); // assuming 15km/h speed
        expectedOrder.add(new Pair<Long, LocalTime>(2L, time));
        time = time.plusSeconds(300); // pickup duration
        time = time.plusSeconds((long) (3600.0 * 300.0 / 15000.0));
        expectedOrder.add(new Pair<Long, LocalTime>(1L, time));
        time = time.plusSeconds(200); // delivery duration
        time = time.plusSeconds((long) (3600.0 * (300.0 + 350.0) / 15000.0));
        expectedOrder.add(new Pair<Long, LocalTime>(0L, time));

        Assert.assertEquals(expectedOrder, tsp.getSolutionOrderWithArrivalTime());
    }

    @Test
    public void getCoutSolution_ShouldGiveSolutionCost() {
        tsp.chercheSolution();

        // Total cost: 250 + 300 + 300 + 300 + 350 = 1500.0
        double expectedCost = 1500.0;
        Assert.assertEquals(expectedCost, tsp.getCoutSolution(), 0.001);

        // LinkedHashSet<Map<Pair<Long, Long>, LinkedList<Long>>> expectedPath = new
        // LinkedHashSet<>();
        // LinkedList<Long> path0to2 = new LinkedList<>();
        // path0to2.add(0L);
        // path0to2.add(1L);
        // path0to2.add(2L);
        // expectedPath.add(Map.of(new Pair<>(0L, 2L), path0to2));
        // LinkedList<Long> path2to1 = new LinkedList<>();
        // path2to1.add(2L);
        // path2to1.add(1L);
        // expectedPath.add(Map.of(new Pair<>(2L, 1L), path2to1));
        // LinkedList<Long> path1to0 = new LinkedList<>();
        // path1to0.add(1L);
        // path1to0.add(2L);
        // path1to0.add(0L);
        // expectedPath.add(Map.of(new Pair<>(1L, 0L), path1to0));

        // Assert.assertEquals(expectedPath, tsp.getSolutionPath());
    }

}
