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

import backend.TSP.TSP;
import backend.TSP.TSP2;
import backend.TSP.Graph;
import backend.models.Edge;
import backend.models.Node;
import backend.models.Pair;
import backend.models.Response;
import backend.models.PointOfInterest;

@SpringBootApplication
public class Server {

    @PostMapping("/runTSP")
    public ResponseEntity<?> runTSP(@RequestBody Map<Long, PointOfInterest> all_nodes,
                                        @RequestBody List<Edge> all_edges,
                                        @RequestBody Map<Long, PointOfInterest> tour) {
        

        LocalTime time = LocalTime.of(8, 0); // 8:00 AM
        //public static void main(String[] args) {
        //SpringApplication.run(Server.class, args);

        TSP tsp = new TSP2();

        //TODO : recuperer all_nodes, all_edges and tour from frontend
        //Map<Long, PointOfInterest> all_nodes = new HashMap<>();
		Graph g = new Graph(all_nodes, all_edges, tour);

		long tempsDebut = System.currentTimeMillis();
		tsp.chercheSolution(60000, g);

        System.out.print("Solution de longueur "+tsp.getCoutSolution()+" trouvee en "
				+(System.currentTimeMillis() - tempsDebut)+"ms : ");

        Pair<Long, LocalTime>[] bestSolution = new Pair[g.getNbPoI()+1];
        for (int i=0; i<g.getNbPoI()+1; i++) {
            long nodeId = tsp.getSolution(i);
            time = time.plusSeconds( all_nodes.get(nodeId).getDuration());
            bestSolution[i] = new Pair<Long, LocalTime>(nodeId, time);
        }
        //TODO : retourner bestSolution ET g.getPredecesseurs()

        return new ResponseEntity<Object>(new Response(bestSolution, g.getPredecesseurs()),HttpStatus.OK);

    } 
}