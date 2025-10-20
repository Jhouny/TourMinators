package backend;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import backend.TSP.TSP2;
import backend.TSP.Graph;
import backend.models.Edge;
import backend.models.Node;
import backend.models.PointOfInterest;

@RestController
public class Server {

    @PostMapping("/runTSP")
    public ResponseEntity<?> runTSP(@RequestBody TSPRequest tspRequest) {

        System.out.println("Received TSP request");
        System.out.println("All Nodes: " + tspRequest.getAllNodes());
        System.out.println("All Edges: " + tspRequest.getAllEdges());
        System.out.println("Tour: " + tspRequest.getTour());

        Map<Long, Node> all_nodes = tspRequest.getAllNodes(); 
        List<Edge> all_edges = tspRequest.getAllEdges();
        List<PointOfInterest> tour = tspRequest.getTour();

        // Pretty print the tour
        System.out.println("Tour Points of Interest:");
        for (PointOfInterest poi : tour) {
            Long poiId = poi.getNode().getId();
            System.out.println("PoI ID: " + poiId + ", Type: " + poi.getType() + ", Associated ID: " + poi.getAssociatedPoI());
        }

        Graph g = new Graph(all_nodes, all_edges, tour);
        LocalTime time = LocalTime.of(8, 0); // 8:00 AM - default start time

        // The brute-force approach will compute the optimal path from each PoI to every other PoI
        //     It'll then make sure that all pickups are done before their corresponding deliveries
        //     Then order them such that the total travel cost is minimized
        BruteForceTSP solver = new BruteForceTSP(g);
        solver.solve();

        // Get the solution order and paths
        ArrayList<Long> solutionOrder = solver.getSolutionOrder();
        ArrayList<Map<Long, Long>> solutionPaths = solver.getSolutionPaths();

        // Log the solution
        System.out.println("TSP Solution Order: " + solutionOrder);
        System.out.println("TSP Solution Paths: " + solutionPaths);

        // Return the solution in the response
        Map<String, Object> response = new HashMap<>();
        response.put("solutionOrder", solutionOrder);
        response.put("solutionPaths", solutionPaths);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}