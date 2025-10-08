package tsp_pnd;

import java.util.Map;
import java.util.HashMap;

public class RunTSP {
	
	public static void main(String[] args) {
		TSP tsp = new TSP2();

		Map<Long, Node> sommets = new HashMap<>();
		sommets.put(Long.valueOf(0), new Node(0, 45.75406, 4.877418)); // entrepot
		sommets.put(Long.valueOf(1), new Node(1, 45.750404, 4.8744674));
		sommets.put(Long.valueOf(2), new Node(2, 45.75871, 4.8704023));
		sommets.put(Long.valueOf(3), new Node(3, 45.75171, 4.8718166));

		long entrepot = 0;

		Graphe g = new GrapheDistances(sommets, Long.valueOf(0));
		long tempsDebut = System.currentTimeMillis();
		tsp.chercheSolution(60000, g);
		System.out.print("Solution de longueur "+tsp.getCoutSolution()+" trouvee en "
				+(System.currentTimeMillis() - tempsDebut)+"ms : ");
		for (int i=0; i<sommets.size(); i++)
			System.out.print(tsp.getSolution(i)+" ");
		System.out.println();
		
	}
}
