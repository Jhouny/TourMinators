package tsp_pnd;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;

//import backend.models.Node;

public class GrapheDistances implements Graphe {
	
	int nbSommets;
	Map<Long, Node> sommets;
	Map<Set<Long> , Double> cout;
	Long beginId;
	
	/**
	 * Cree un graphe complet dont les aretes ont un cout compris entre COUT_MIN et COUT_MAX
	 * @param nbSommets
	 */
	public GrapheDistances(Map<Long, Node> sommets, Long beginId){
		this.cout = new HashMap<Set<Long> , Double>();
		this.sommets = sommets;
		this.beginId = beginId;
		this.nbSommets = sommets.size();
	}

	@Override
	public ArrayList<Long> getNodesToVisit() {
		ArrayList<Long> nodesToVisit = new ArrayList<Long>();
		for (Long id : sommets.keySet()) {
			nodesToVisit.add(id);
		}
		nodesToVisit.remove(getBeginId());
		return nodesToVisit;
	}

	@Override
	public Long getBeginId() {
		return beginId;
	}

	@Override
	public int getNbSommets() {
		return nbSommets;
	}

	@Override
	public double getCout(Long i, Long j) {
		Set<Long> pair = new HashSet<Long>();
		pair.add(i);
		pair.add(j);
		if (cout.containsKey(pair)) {
			return cout.get(pair);
		}
		else{
			double distance = (double) Math.pow((sommets.get(i).getLat() - sommets.get(j).getLat()), 2) + Math.pow((sommets.get(i).getLong() - sommets.get(j).getLong()), 2);
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
