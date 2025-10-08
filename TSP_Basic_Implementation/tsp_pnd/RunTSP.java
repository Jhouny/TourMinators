package tsp_pnd;

import java.util.Map;
import java.util.HashMap;

public class RunTSP {
	
	public static void main(String[] args) {
		TSP tsp = new TSP2();

		Map<Long, Pair<Node, Long>> sommets = new HashMap<>();
		sommets.put(Long.valueOf(0), new Pair<Node, Long>(new Node(0, 45.75406, 4.877418), null)); // entrepot
		sommets.put(Long.valueOf(1), new Pair<Node, Long>(new Node(1, 45.750404, 4.8744674), Long.valueOf(2)));// pickup 1, delivery 2	
		sommets.put(Long.valueOf(2), new Pair<Node, Long>(new Node(2, 45.75871, 4.8704023), null));
		sommets.put(Long.valueOf(3), new Pair<Node, Long>(new Node(3, 45.75171, 4.8718166), Long.valueOf(4)));// pickup 3, delivery 4
		sommets.put(Long.valueOf(4), new Pair<Node, Long>(new Node(4, 45.755147, 4.8739586), null));

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
