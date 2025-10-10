package backend.TSP;

import java.util.Map;
import java.util.Set;

import backend.models.Graphe;
import backend.models.Node;
import backend.models.Pair;

import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;


//import backend.models.Node;

public class GrapheDistances implements Graphe {
	
	int nbSommets;
	Map<Long, Pair<Node, Long>> sommets; // node id -> (Node, deliveryId) (deliveryId = null si le noeud n'est pas un pickup)
	Map<Set<Long> , Double> cout;
	Long beginId;
	
	/**
	 * Cree un graphe complet dont les aretes ont un cout compris entre COUT_MIN et COUT_MAX
	 * @param nbSommets
	 */
	public GrapheDistances(Map<Long, Pair<Node, Long>> sommets, Long beginId){
		this.cout = new HashMap<Set<Long> , Double>();
		this.sommets = sommets;
		this.beginId = beginId;
		this.nbSommets = sommets.size();
	}

	@Override
	public ArrayList<Long> getNodesToVisit() {
		ArrayList<Long> nodesToVisit = new ArrayList<Long>();
		for (Long id : sommets.keySet()) {
			if (sommets.get(id).getRight() != null){
				nodesToVisit.add(id);
			}
		}
		return nodesToVisit;
	}

	@Override
	public Long getDelivery(Long id) {
		if (sommets.get(id).getRight() != null){
			return sommets.get(id).getRight();
		}
		return null;
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
			double distance = (double) Math.sqrt(Math.pow((sommets.get(i).getLeft().getLat() - sommets.get(j).getLeft().getLat()), 2) + Math.pow((sommets.get(i).getLeft().getLong() - sommets.get(j).getLeft().getLong()), 2));
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
