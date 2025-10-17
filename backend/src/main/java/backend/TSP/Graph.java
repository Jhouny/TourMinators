package backend.TSP;

import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import backend.models.Edge;
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

public class Graph  {
	
	List<Edge> all_edges;
	Map<Long, Node> all_nodes;
	Map<Pair<Long, Long>, Float> all_costs; // If there is no edge between i and j, there is no entry (i,j) in this map and an exception is thrown

	Map<Long, PointOfInterest> tour;
	Map<Pair<Long, Long> , Float> pathCost;
	Map<Long, Set<Long>> adjacency; 
	
	
	public Graph(Map<Long, Node> nodes, List<Edge> edges, Map<Long, PointOfInterest> tour){
        // Initialize graph attributes
        this.all_nodes = nodes; 
        this.all_edges = edges;
        this.tour = tour;

        this.pathCost = new HashMap<Pair<Long, Long> , Float>(); // Empty - to be filled when WA* called
        this.adjacency = new HashMap<Long, Set<Long>>();
        this.all_costs = new HashMap<Pair<Long, Long> , Float>();

        // Build adjacency and cost tables
        for (Edge edge : edges) {
            Long origin = edge.getOrigin();
            Long destination = edge.getDestination();
            float distance = edge.getLength();

            // Build adjacency
            // computeIfAbsent is a lambda key function to add a Set to the adjancency if it doesnt exist
            adjacency.computeIfAbsent(origin, k -> new HashSet<>()).add(destination);

            // Store edge cost (unordered pair)
            Pair<Long, Long> pair = new Pair<Long, Long>(origin, destination);
            all_costs.put(pair, distance);
        }
	}

	public ArrayList<Long> getNodesToVisit() {
        // Return the list of pickup nodes to visit (not deliveries, not warehouse)
		ArrayList<Long> nodesToVisit = new ArrayList<Long>();
		for (Long id : tour.keySet()) {
			if (tour.get(id).getType() == PointOfInterest.PoIEnum.PICKUP){
				nodesToVisit.add(id);
			}
		}
		return nodesToVisit;
	}

	public Long getAssociatedPoI(Long id) {
        // Return the associated PoI of a pickup/delivery node (null if warehouse)
        Long associatedPoI = null;
        if (tour.containsKey(id))
            associatedPoI = tour.get(id).getAssociatedPoI();
        return associatedPoI;
	}

	public Long getBeginId() {
        // Return the id of the warehouse
        Long warehouseId = null;
        for (Long id : tour.keySet()) {
			if (tour.get(id).getType() == PointOfInterest.PoIEnum.WAREHOUSE){
				warehouseId = id;
                break;
			}
		}
        // tester si warehouse est null
		return warehouseId;
	}

	public int getNbNodes() {
		return this.all_nodes.size();
	}

    public Float getCost(Long i, Long j) {
        //Returns the cost between 2 nodes, or throws an exception if there is no edge between them
        Pair<Long, Long> pair = new Pair<Long, Long>(i, j);
        if (all_costs.containsKey(pair)) {
            return all_costs.get(pair);
        } else {
            throw new IllegalArgumentException("No edge between " + i + " and " + j);
        }
    }

	public boolean isEdge(Long i, Long j) {
        // Returns true if there is an edge between i and j, false otherwise
		if (tour.containsKey(i) && tour.containsKey(j))
			return !i.equals(j);
		return false;
	}

	//=========================== AWA ======================================//
	public Float getPathCost(Long i, Long j) {
        // Returns the cost of the optimal path between i and j, or null if AWA* has not been called for this pair
        return pathCost.get(new Pair<Long, Long>(i, j));
    }

	public Set<Long> getNeighbors(long i) {
        //GetOrDefault returns either adjacency.get(i) if not null, either an empty Set if i is null
        return adjacency.getOrDefault(i, Collections.emptySet());
    }

    private float heuristic(Long i, Long j) {
        int weight = 1;
        Node nodeI = all_nodes.get(i);
        Node nodeJ = all_nodes.get(j);
        // Current heuristic : euclidian distance
        return weight * ((int) Math.sqrt(Math.pow(nodeI.getLat() - nodeJ.getLat(), 2) + Math.pow(nodeI.getLong() - nodeJ.getLong(), 2)));
    }

	private void printSolution(long startId, long endId, Map<Long, Float> costMap, Map<Long, Long> cameFrom) {
		// TODO: Test and implement properly

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

    private void printd(String s) {
        boolean debug = true;
        if (debug)
            System.out.println(s);
    }

	public Map<Long, Long> AWAStar(Long startId, Long endId) {
        // This function returns the mapping of predecessors 
        // and modifies the pathCost attribute to store the cost of the optimal path between startId and endId
        // If there is no path between startId and endId, the cost is set to null

        int nbIter = 0;

        if(getNeighbors(startId).isEmpty() || getNeighbors(endId).isEmpty()){
            printd("No neighbors for start or end node.");
            // If either the startId or the endId have no outgoing neighbors, there is no path between them
            // It's important that we're able to get out of the end node as well to go back to warehouse

            pathCost.put(new Pair<Long, Long>(startId, endId), null);
            return null;
        }

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
                pathCost.put(new Pair<Long, Long>(startId, endId), costMap.get(endId));
                printd("Path found from " + startId + " to " + endId + " with cost " + costMap.get(endId));
                return cameFrom;
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
        pathCost.put(new Pair<Long, Long>(startId, endId), null);
        printd("No path found from " + startId + " to " + endId);
        return cameFrom;
    }




}
