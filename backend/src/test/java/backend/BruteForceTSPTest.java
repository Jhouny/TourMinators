package backend;

import backend.models.Node;
import backend.models.PointOfInterest;
import backend.models.Edge;
import backend.TSP.Graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.junit.Assert;

import backend.TSP.BruteForceTSP;

public class BruteForceTSPTest {
    // Test cases for BruteForceTSP
    @Test
    public void testExample() {
        // Example test case
        Map<Long, Node> nodes = new HashMap<>();
        nodes.put(1L, new Node(1, 48.8566, 2.3522)); // Paris
        nodes.put(2L, new Node(2, 45.7640, 4.8357)); // Lyon
        nodes.put(3L, new Node(3, 47.2186, -1.5536)); // Nantes

        List<Edge> edges = new ArrayList<>();
        edges.add(new Edge(1L, 2L, 777, "paris-lyon")); // Paris to Lyon
        edges.add(new Edge(2L, 1L, 777, "lyon-paris")); // Lyon to Paris

        edges.add(new Edge(1L, 3L, 385, "paris-nantes")); // Paris to Nantes
        edges.add(new Edge(3L, 1L, 385, "nantes-paris")); // Nantes to Paris

        edges.add(new Edge(2L, 3L, 593, "lyon-nantes")); // Lyon to Nantes
        edges.add(new Edge(3L, 2L, 593, "nantes-lyon")); // Nantes to Lyon

        Map<Long, PointOfInterest> tour = new HashMap<>();
        // Add PoIs to the tour as needed for testing
        tour.put(1L, new PointOfInterest(nodes.get(1L), PointOfInterest.PoIEnum.warehouse, 0L, 0));
        tour.put(2L, new PointOfInterest(nodes.get(2L), PointOfInterest.PoIEnum.pickup, 3L, 0)); // Pickup for delivery 3
        tour.put(3L, new PointOfInterest(nodes.get(3L), PointOfInterest.PoIEnum.delivery, 2L, 0));

        Graph graph = new Graph(nodes, edges, tour);
        BruteForceTSP tsp = new BruteForceTSP(graph);

        Set<Long> PoIs = tsp.retrievePoIs();
        Assert.assertEquals("Expected 3 PoIs", 3, PoIs.size());

        ArrayList<ArrayList<Long>> permutations = tsp.generatePermutations(new ArrayList<>(PoIs));
        Assert.assertEquals("Expected 2 permutations for 3 PoIs (warehouse + one pickup-delivery pair)", 2, permutations.size());
        
        // Add warehouse return to each permutation
        for (ArrayList<Long> perm : permutations) {
            perm.add(perm.get(0)); // Return to warehouse
        }

        int validCount = 0;
        for (ArrayList<Long> perm : permutations) {
            System.out.println("Testing permutation: " + perm);
            if (tsp.isValidSolution(perm)) {
                validCount++;
            }
        }
        Assert.assertEquals("Expected 1 valid permutation", 1, validCount);

        tsp.solve();
        ArrayList<Long> solutionOrder = tsp.getSolutionOrder();
        ArrayList<Map<Long, Long>> solutionPaths = tsp.getSolutionPaths();

        Assert.assertTrue(solutionOrder.size() > 0);
        Assert.assertEquals("Expected solution paths size to be one less than order size", solutionOrder.size() - 1, solutionPaths.size());
    }
}
