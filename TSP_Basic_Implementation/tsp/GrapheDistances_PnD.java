package tsp;

import java.util.Map;
import backend.models.Node;

public class GrapheDistances_PnD implements Graphe {
	
	int nbSommets;
	Map<long, Node> sommets;
	Map<Set<long> , int> cout;
	
	/**
	 * Cree un graphe complet dont les aretes ont un cout compris entre COUT_MIN et COUT_MAX
	 * @param nbSommets
	 */
	public GrapheComplet(Map<long, Node> sommets){
		this.cout = new HashMap<>();
		this.sommets = sommets;
		this.nbSommets = sommets.size();
	}

	@Override
	public int getNbSommets() {
		return nbSommets;
	}

	@Override
	public int getCout(long i, long j) {
		Set<long> pair = new Set<long>(i,j)
		if (cout.containsKey(pair)) {
			return cout.get(pair);
		}
		else{
			int distance = (int) Math.sqrt(Math.pow((sommets.get(i).getLat() - sommets.get(j).getCoutSolution()), 2) + Math.pow((sommets.get(i).getLong() - sommets.get(j).getLong()), 2));
			cout.put(pair, distance);
			return distance;
		}
	}

	@Override
	public boolean estArc(long i, long j) {
		if (sommets.containsKey(i) && sommets.containsKey(j))
			return i != j;
		return false;
	}

}
