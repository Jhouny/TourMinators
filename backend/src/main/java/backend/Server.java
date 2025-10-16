package backend;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import backend.TSP.TSP;
import backend.TSP.TSP2;
import backend.TSP.Graph;
import backend.models.Node;
import backend.models.Pair;
import backend.models.PointOfInterest;

@SpringBootApplication
public class Server {
    private static LocalTime time = LocalTime.of(8, 0); // 8:00 AM
    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);

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

    } 
}