package tsp_pnd;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

//import backend.models.Node;

public class GrapheDistances implements Graphe {
	
	int nbSommets;
	Map<Long, Node> sommets;
	Map<Set<Long> , Integer> cout;
	
	/**
	 * Cree un graphe complet dont les aretes ont un cout compris entre COUT_MIN et COUT_MAX
	 * @param nbSommets
	 */
	public GrapheDistances(Map<Long, Node> sommets){
		//this.cout = new Map<Set<Long> , Integer>();
		this.sommets = sommets;
		this.nbSommets = sommets.size();
	}

	@Override
	public int getNbSommets() {
		return nbSommets;
	}

	@Override
	public int getCout(Long i, Long j) {
		Set<Long> pair = new HashSet<Long>();
		pair.add(i);
		pair.add(j);
		if (cout.containsKey(pair)) {
			return cout.get(pair);
		}
		else{
			int distance = (int) Math.sqrt(Math.pow((sommets.get(i).getLat()*1e6 - sommets.get(j).getLat()*1e6), 2) + Math.pow((sommets.get(i).getLong()*1e6 - sommets.get(j).getLong()*1e6), 2));
			cout.put(pair, distance);
			System.out.println("distance entre "+sommets.get(i).getId()+" et "+sommets.get(j).getId()+" : "+distance+"  ");
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
