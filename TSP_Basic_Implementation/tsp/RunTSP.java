package tsp;

public class RunTSP {
	
	public static void main(String[] args) {
		TSP tsp;
		switch (args[0]) {
			case "1":
				tsp = new TSP1();
				break;
			case "2":
				tsp = new TSP2();
				break;
			default :
				tsp = new TSP3();
				break;
		}

		for (int nbSommets = 8; nbSommets <= 22; nbSommets += 2){
			System.out.println("Graphes de "+nbSommets+" sommets :");
			Graphe g = new GrapheComplet(nbSommets);
			long tempsDebut = System.currentTimeMillis();
			tsp.chercheSolution(60000, g);
			System.out.print("Solution de longueur "+tsp.getCoutSolution()+" trouvee en "
					+(System.currentTimeMillis() - tempsDebut)+"ms : ");
			for (int i=0; i<nbSommets; i++)
				System.out.print(tsp.getSolution(i)+" ");
			System.out.println();
		}
	}
}
