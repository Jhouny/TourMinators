package backend;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.ObjectMapper;

import backend.TSP.TSP;
import backend.TSP.TSP2;
import backend.TSP.Graph;
import backend.models.Edge;
import backend.models.Node;
import backend.models.Pair;
import backend.models.PointOfInterest;

@RestController
public class Server {

    @PostMapping("/runTSP")
    public ResponseEntity<?> runTSP(@RequestParam("all_nodes") String allNodesJson,
            @RequestParam("all_edges") String allEdgesJson,
            @RequestParam("tour") String tourJson) {

        ObjectMapper mapper = new ObjectMapper();

        Map<String, Object> allNodesRaw = mapper.readValue(allNodesJson, new TypeReference<Map<String, Object>>() {
        });
        List<Map<String, Object>> allEdgesRaw = mapper.readValue(allEdgesJson,
                new TypeReference<List<Map<String, Object>>>() {
                });
        Map<String, Object> tourRaw = mapper.readValue(tourJson, new TypeReference<Map<String, Object>>() {
        });

        Map<Long, Node> all_nodes = new HashMap<>();
        for (var entry : allNodesRaw.entrySet()) {
            Long id = Long.parseLong(entry.getKey());
            Map<String, Object> nodeMap = (Map<String, Object>) entry.getValue();
            Node node = new Node(id,
                    ((Number) nodeMap.get("latitude")).doubleValue(),
                    ((Number) nodeMap.get("longitude")).doubleValue());
            all_nodes.put(id, node);
        }

        TSP tsp = new TSP2();
        Graph g = new Graph(all_nodes, all_edges, tour);
        LocalTime time = LocalTime.of(8, 0); // 8:00 AM

        long tempsDebut = System.currentTimeMillis();
        tsp.chercheSolution(60000, g);

        System.out.print("Solution de longueur " + tsp.getCoutSolution() + " trouvee en "
                + (System.currentTimeMillis() - tempsDebut) + "ms : ");

        Pair<Long, LocalTime>[] bestSolution = new Pair[tour.size() + 1]; // +1 for return to warehouse

        long previousNodeId = tsp.getSolution(0);
        long nodeId = tsp.getSolution(0);
        bestSolution[0] = new Pair<Long, LocalTime>(nodeId, time);

        for (int i = 1; i < tour.size() + 1; i++) {
            previousNodeId = nodeId;
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
        }

        // TODO : retourner bestSolution ET g.getPredecesseurs()
        Pair<Long, LocalTime>[] bestSolution = new Pair[g.getNbPoI() + 1];
        for (int i = 0; i < g.getNbPoI() + 1; i++) {
            long nodeId = tsp.getSolution(i);
            time = time.plusSeconds(all_nodes.get(nodeId).getDuration());
            bestSolution[i] = new Pair<Long, LocalTime>(nodeId, time);
        }
        // TODO : retourner bestSolution ET g.getPredecesseurs()

        Map<String, Object> responseBody = Map.of(
                "bestSolution", bestSolution,
                "predecesseurs", g.getPredecesseurs());

        return new ResponseEntity<>(responseBody, HttpStatus.OK);

    }
}