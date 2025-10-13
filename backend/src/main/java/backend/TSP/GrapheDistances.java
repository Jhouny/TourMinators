package backend.TSP;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import backend.TSP.GraphAWA.NodeCost;
import backend.models.Edge;
import backend.models.Graph;
import backend.models.Node;
import backend.models.Pair;
import backend.models.PointOfInterest;
import backend.models.NodeWithCost;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

import backend.models.PointOfInterest;

public class GrapheDistances implements Graph {
	
	List<Edge> all_edges;
	Map<Long, Node> all_nodes;
	Map<Set<Long>, Float> all_costs;//KO : Set is not ordered !!!

	Map<Long, PointOfInterest> tour;
	Map<Set<Long> , Float> pathCost;//KO : Set is not ordered !!!
	Map<Long, Set<Long>> adjacency; //KO : Set is not ordered !!!
	
	/**
	 * Cree un graphe complet dont les aretes ont un cout compris entre COUT_MIN et COUT_MAX
	 * @param nbSommets
	 */

	// public GrapheDistances(Map<Long, Pair<Node, Long>> nodes, Long beginId){
	// 	this.cout = new HashMap<Set<Long> , Double>();
	// 	this.sommets = sommets;
	// 	this.beginId = beginId;
	// 	this.nbSommets = sommets.size();
	// }
	
	public GrapheDistances(Map<Long, Node> nodes, List<Edge> edges, Map<Long, PointOfInterest> tour){
        this.all_nodes = nodes; 
        this.all_edges = edges;
        this.tour = tour;

        this.pathCost = new HashMap<Set<Long> , Float>();
        this.adjacency = new HashMap<Long, Set<Long>>();
        this.all_costs = new HashMap<Set<Long> , Float>();

        // Build adjacency and cost tables
        for (Edge edge : edges) {
            long origin = edge.getOrigin();
            long destination = edge.getDestination();
            float distance = edge.getLength();

            // Build adjacency
            // computeIfAbsent is a lambda key function to add a Set to the adjancency if it doesnt exist
            adjacency.computeIfAbsent(origin, k -> new HashSet<>()).add(destination);
            adjacency.computeIfAbsent(destination, k -> new HashSet<>()).add(origin);

            // Store edge cost (unordered pair)
            Set<Long> pair = new HashSet<>(Arrays.asList(origin, destination));
            all_costs.put(pair, distance);
        }
	}

	public ArrayList<Long> getNodesToVisit() {
		ArrayList<Long> nodesToVisit = new ArrayList<Long>();
		for (Long id : tour.keySet()) {
			if (tour.get(id).getType() == PointOfInterest.PoIEnum.PICKUP){
				nodesToVisit.add(id);
			}
		}
		return nodesToVisit;
	}

	public Long getAssociatedPoI(Long id) {
        return tour.get(id).getAssociatedPoI();
	}

	public Long getBeginId() {
        Long warehouseId = null;
        for (Long id : tour.keySet()) {
			if (tour.get(id).getType() == PointOfInterest.PoIEnum.WAREHOUSE){
				warehouseId = id;
                break;
			}
		}
		return warehouseId;
	}

	@Override
	public int getNbNodes() {
		return this.all_nodes.size();
	}

    // @Override
    // public float getCost(long i, long j) {

    //     Set<Long> pair = new HashSet<>(Arrays.asList(i, j));
    //     if (costs.containsKey(pair)) {
    //         return costs.get(pair);
    //     } else {
    //         throw new IllegalArgumentException("No edge between " + i + " and " + j);
    //     }
    // }


	@Override
	public boolean isEdge(long i, long j) {
		if (tour.containsKey(i) && tour.containsKey(j))
			return i != j;
		return false;
	}

	//===========================AWA - Methodes de Vini======================================//
	
	public Set<Long> getNeighbors(long i) {
        //GetOrDefault renvoie soit i si pas nul, soit un Set vide si i est nul
        return adjacency.getOrDefault(i, Collections.emptySet());
    }

    private float heuristic(long i, long j) {
        int weight = 1;
        Node nodeI = all_nodes.get(i);
        Node nodeJ = all_nodes.get(j);
        // Utilisation de la distance euclidienne comme heuristique
        return weight * ((int) Math.sqrt(Math.pow(nodeI.getLat() - nodeJ.getLat(), 2) + Math.pow(nodeI.getLong() - nodeJ.getLong(), 2)));
    }

	private void printSolution(long startId, long endId, Map<Long, Float> costMap, Map<Long, Long> cameFrom) {
		// TODO: Test and implement properlys

        // Reconstruct the path from end to start using cameFrom
        List<Long> path = new ArrayList<>();
        Long current = endId;

        while (current != null) {
            path.add(current);
            current = cameFrom.get(current); // move to predecessor
        }

        // Reverse the path so it goes from start to end
        Collections.reverse(path);

        // Compute total cost
        float totalCost = 0f;
        for (int i = 0; i < path.size() - 1; i++) {
            long from = path.get(i);
            long to = path.get(i + 1);
            totalCost += getCost(from, to); // cumulative cost along the path
        }

        // Print path and total cost
        System.out.println("Optimal path: " + path);
        System.out.println("Total path cost: " + totalCost);
    }



	public int AWAStar(long startId, long endId) {

        int nbIter = 0;

        Map<Long, Float> costMap  = new HashMap<>(); // cout cumulatif pour chaque noeud
        Map<Long, Long> cameFrom = new HashMap<>(); // prédecesseur (pour reconstruire le chemin)
        Set<Long> visited = new HashSet<>();

        //File de priorite pour stocker les noeuds a visiter
        PriorityQueue<NodeWithCost> q = new PriorityQueue<>();

        NodeWithCost startNode = new NodeWithCost(startId, heuristic(startId, endId));
        q.add(startNode);

        costMap.put(startId, 0f);
        cameFrom.put(startId, null);

        while(!q.isEmpty()){
            nbIter++;
            NodeWithCost current = q.poll();

            if (visited.contains(current.getId()))
                continue;
            
            if(current.getId() == endId){
                System.out.println("Nombre d'itérations : " + nbIter);
                printSolution(startId, endId, costMap, cameFrom);
                return 0;
            }

            for(long neighbor : getNeighbors(current.getId())){
                float newCost = costMap.get(current.getId()) + getCost(current.getId(), neighbor);
                float h = heuristic(neighbor, endId);
                float f = newCost + h;

                if (!costMap.containsKey(neighbor) || newCost < costMap.get(neighbor)) {
                    costMap.put(neighbor, newCost);
                    cameFrom.put(neighbor, current.getId());
                    q.add(new NodeWithCost(neighbor, f));
                }
            }

            visited.add(current.getId());
        }

        return 0;
    }




}
