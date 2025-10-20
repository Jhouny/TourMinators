package backend;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import backend.TSP.Graph;
import backend.TSP.TSP;
import backend.TSP.TSP2;
import backend.models.Edge;
import backend.models.Node;
import backend.models.Pair;
import backend.models.PointOfInterest;
import backend.models.TSPRequest;

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
        Map<Long, PointOfInterest> tour = tspRequest.getTour();

        TSP tsp = new TSP2();

        Graph g = new Graph(all_nodes, all_edges, tour);
        LocalTime time = LocalTime.of(8, 0); // 8:00 AM - default start time

        long tempsDebut = System.currentTimeMillis();
        tsp.chercheSolution(60000, g);

        System.out.print("Solution de longueur " + tsp.getCoutSolution() + " trouvee en "
                + (System.currentTimeMillis() - tempsDebut) + "ms : ");

        @SuppressWarnings("unchecked")
        Pair<Long, LocalTime>[] bestSolution = (Pair<Long, LocalTime>[]) new Pair[tour.size() + 1]; // +1 for return to warehouse
        
        Map <Pair<Long,Long>, Map<Long, Long>> predecessors = new HashMap<>();

        long previousNodeId = tsp.getSolution(0);
        long nodeId = tsp.getSolution(0);
        bestSolution[0] = new Pair<Long, LocalTime>(nodeId, time);

        for (int i = 1; i < tour.size() + 1; i++) {
            previousNodeId = nodeId;

            //fill in bestSolution
            if (i != tour.size())
                nodeId = tsp.getSolution(i);
            else
                nodeId = tsp.getSolution(0);

            if (i != 1)
                time = time.plusSeconds(tour.get(previousNodeId).getDuration());// add duration of previous PoI
            time = time.plusSeconds((long) (g.getPathCost(previousNodeId, nodeId) * 3600 / 15000));// add travel time
                                                                                                   // between previous
                                                                                                   // and current PoI,
                                                                                                   // assuming 15km/h
                                                                                                   // speed
            bestSolution[i] = new Pair<Long, LocalTime>(nodeId, time);

            //fill in predecessors
            Map<Long, Long> preds = g.AWAStar(previousNodeId, nodeId);
            predecessors.put(new Pair<Long, Long>(previousNodeId, nodeId), preds);
        }

        //return bestSolution and g.getPredecesseurs()

        Map<String, Object> responseBody = Map.of(
                "bestSolution", bestSolution,
                "predecesseurs", predecessors);
        return new ResponseEntity<>(responseBody, HttpStatus.OK);
        
    }
}