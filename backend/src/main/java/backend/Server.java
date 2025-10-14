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

@SpringBootApplication
public class Server {
    private static LocalTime startTime = LocalTime.of(8, 0); // 8:00 AM
    public static void main(String[] args) {
        SpringApplication.run(Server.class, args);

        TSP tsp = new TSP2();

        //TODO : recuperer all_nodes, all_edges and tour from frontend

		Graph g = new Graph(all_nodes, all_edges, tour);

		long tempsDebut = System.currentTimeMillis();
		tsp.chercheSolution(60000, g);

        System.out.print("Solution de longueur "+tsp.getCoutSolution()+" trouvee en "
				+(System.currentTimeMillis() - tempsDebut)+"ms : ");

        Pair<Long, LocalTime>[] bestSolution = new Pair[g.getNbPoI()+1];
        for (int i=0; i<g.getNbPoI()+1; i++) {
            long nodeId = tsp.getSolution(i);
            LocalTime arrivalTime = ... //TODO : compute arrival time at nodeId



        //TODO : retourner bestSolution (complète) ET g.getPredecesseurs()

    } 
}