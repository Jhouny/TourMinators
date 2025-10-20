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

import backend.TSP.Graph;
import backend.models.Edge;
import backend.models.Node;
import backend.models.PointOfInterest;
import backend.models.MultipleDeliverersRequest;

@RestController
public class Server {

    @PostMapping("/runTSP")
    public ResponseEntity<?> runTSP(@RequestBody MultipleDeliverersRequest deliverersRequest) {

        System.out.println("Received TSP request");
        System.out.println("Deliverer Assignments: " + deliverersRequest.getDelivererAssignments());

        Map<Long, Node> all_nodes = deliverersRequest.getAllNodes();
        List<Edge> all_edges = deliverersRequest.getAllEdges();

        // Deliverers are not created here automatically, need KeyDeserializer
        Map<String, Map<Long, PointOfInterest>> delivererAssignments = deliverersRequest.getDelivererAssignments();

        Map<String, Map<String, Object>> allDeliverersResults = new HashMap<>();

        for (Map.Entry<String, Map<Long, PointOfInterest>> entry : delivererAssignments.entrySet()) {
            String delivererName = entry.getKey();
            Map<Long, PointOfInterest> tour = entry.getValue();

            System.out.println("Processing " + delivererName + " with " + tour.size() + " POIs");

            if (tour.isEmpty()) {
                System.out.println(delivererName + " has no deliveries assigned");
                continue;
            }

            // Pretty print the tour
            System.out.println("Tour Points of Interest for " + delivererName + ":");
            for (PointOfInterest poi : tour.values()) {
                Long poiId = poi.getNode().getId();
                System.out.println(
                        "PoI ID: " + poiId +
                                ", Type: " + poi.getType() +
                                ", Associated ID: " + poi.getAssociatedPoI());
            }

            Graph g = new Graph(all_nodes, all_edges, tour);
            LocalTime time = LocalTime.of(8, 0); // 8:00 AM - default start time

            BruteForceTSP solver = new BruteForceTSP(g);
            solver.solve();

            // Get the solution order and paths
            ArrayList<Long> solutionOrder = solver.getSolutionOrder();
            ArrayList<Map<Long, Long>> solutionPaths = solver.getSolutionPaths();

            // Format the response for this deliverer
            Map<String, Object> delivererResult = new HashMap<>();
            delivererResult.put("bestSolution", solutionOrder);
            delivererResult.put("tour", solutionPaths);

            allDeliverersResults.put(delivererName, delivererResult);

            System.out.println("TSP Solution Order for " + delivererName + ": " + solutionOrder);
        }

        // Return all deliverers solutions
        return new ResponseEntity<>(allDeliverersResults, HttpStatus.OK);
    }
}