package backend.TSP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import backend.models.Edge;
import backend.models.Node;
import backend.models.NodeWithCost;
import backend.models.Pair;
import backend.models.PointOfInterest;

/**
 * Graph wrapper used by the TSP solver.
 *
 * Holds nodes, edges, adjacency and cached path costs. Methods provide basic
 * graph queries, a weighted A* search (AWAStar) and simple accessors.
 *
 * Note: several methods may return null to indicate absence of data (for
 * example {@link #getCost(Long, Long)} or cached entries in {@link #pathCost}).
 * Methods that require a tour id will throw {@link IllegalArgumentException}
 * when the id is not present in the tour map.
 */
public class Graph  {

	List<Edge> all_edges;
	Map<Long, Node> all_nodes;
	Map<Pair<Long, Long>, Float> all_costs; // If there is no edge between i and j, there is no entry (i,j) in this map and an exception is thrown

	Map<Long, PointOfInterest> tourOrig;
    List<PointOfInterest> tour;
	Map<Pair<Long, Long> , Float> pathCost;
	Map<Long, Set<Long>> adjacency; 
	
	
    /**
     * Create a Graph instance.
     *
     * @param nodes  map of node id -> {@link Node}; must not be null
     * @param edges  list of graph edges; must not be null
     * @param tour   list of point-of-interest; must not be null
     *
     * All parameters are stored by reference. Null parameters will cause a
     * {@link NullPointerException} during construction.
     */
    public Graph(Map<Long, Node> nodes, List<Edge> edges, Map<Long, PointOfInterest> tour){
        // Initialize graph attributes
        this.all_nodes = nodes; 
        this.all_edges = edges;
        this.tourOrig = tour;

        this.tour = new ArrayList<>(tour.values());

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

    /**
     * Return the PoI type for the given id.
     *
     * @param id id of the PoI
     * @return the PoI enum type
     * @throws IllegalArgumentException if the id is not present in the tour map
     */
    public PointOfInterest.PoIEnum getTypePoI(Long id) {
        for (PointOfInterest poi : tour) {
            if (poi.getId().equals(id)) {
                return poi.getType();
            }
        }

        return null;
    }

    /**
     * Return the associated PoI id for a pickup/delivery node.
     *
     * @param id PoI id
     * @return associated PoI id, or null when the PoI is a warehouse or id is unknown
     */
    public Long getAssociatedPoI(Long id) {
        for (PointOfInterest poi : tour) {
            if (poi.getId().equals(id)) {
                return poi.getAssociatedPoI();
            }
        }
        return null;
	}

    /**
     * Find the first warehouse id in the tour.
     *
     * @return a warehouse id if present; otherwise null
     */
    public Long getBeginId() {
        Long warehouseId = null;
        for (PointOfInterest poi : tour) {
			if (poi.getType() == PointOfInterest.PoIEnum.WAREHOUSE){
				warehouseId = poi.getId();
                break;
			}
		}
  
        if (warehouseId == null) {
            throw new IllegalArgumentException("No warehouse found in the tour.");
        }
		return warehouseId;
	}

    /**
     * Retrieve all the Pickup PoI in the tour
     * @return a list of Pickup PoI
     */
    public LinkedHashSet<Long> getPickupPoIs() {
        LinkedHashSet<Long> pickups = new LinkedHashSet<>();
        for (PointOfInterest poi : tour) {
            if (poi.getType() == PointOfInterest.PoIEnum.PICKUP) {
                pickups.add(poi.getId());
            }
        }
        return pickups;
    }

    /**
     * Return the number of nodes held by this graph.
     *
     * @return the number of entries in {@link #all_nodes}
     * @throws NullPointerException if {@code all_nodes} is null
     */
    public int getNbNodes() {
        return this.all_nodes.size();
    }


    /**
     * Validate 
     * @param i
     * @param j
     * @return
     */

    /**
     * Return the direct edge cost from {@code i} to {@code j} stored in this
     * Graph instance.
     *
     * <p>This method looks up the cost in the internal cost table built from
     * the edges that were provided when this Graph was created. The graph
     * instance represents the full graph loaded into memory for this run, but
     * it may be incomplete or sparse (not every pair of nodes has a direct
     * edge).
     *
     * @param i origin node id
     * @param j destination node id
     * @return the cost as a {@link Float} when a direct edge (i,j) is present
     *         in this graph; otherwise {@code null} when no direct edge is
     *         recorded
     * @throws NullPointerException when {@code i}, {@code j} or internal maps are null
     */
    public Float getCost(Long i, Long j) {
        Pair<Long, Long> pair = new Pair<Long, Long>(i, j);
        if (all_costs.containsKey(pair)) {
            return all_costs.get(pair);
        } else {
            return null;
        }
    }

    /**
     * Lightweight test whether two tour ids can form an edge.
     *
     * <p>This checks only that both ids exist in the {@link #tour} map and
     * that they are not equal. It does not consult the stored cost table;
     * therefore a {@code true} result does not guarantee that a direct edge
     * (with a cost) is recorded in this Graph. Use {@link #getCost} or
     * {@link #getAllCosts} to check for an actual stored edge.
     *
     * @param i first id
     * @param j second id
     * @return {@code true} when both ids are present in the tour and differ;
     *         otherwise {@code false}
     */
    public boolean isEdge(Long i, Long j) {
        if ( all_costs.containsKey(new Pair<Long, Long>(i, j)) )
            return !i.equals(j);
        return false;
    }

    /**
     * Return the cached cost of the optimal path between {@code i} and
     * {@code j} in this Graph instance.
     *
     * <p>If the cost is not cached this method triggers {@link #AWAStar(Long,
     * Long)} which computes an optimal path using the internal adjacency and
     * cost tables belonging to this Graph object. If no path exists (according
     * to the graph loaded in this object) the method throws
     * {@link IllegalArgumentException}.
     *
     * @param i origin id
     * @param j destination id
     * @return the Float cost of the optimal path
     * @throws IllegalArgumentException when AWAStar determines there is no path
     * @throws NullPointerException when parameters or internal maps are null
     */
    public Float getPathCost(Long i, Long j) {
        Map<Long, Long> cameFrom = null;
        Pair<Long, Long> pair = new Pair<Long, Long>(i, j);
        if(pathCost.get(pair) == null)
            cameFrom = AWAStar(i, j);

        Float cost = pathCost.get(pair);
        if(cost == null){
            String msg = "No path between " + i + " and " + j + ".";
            if (cameFrom != null) {
                StringBuilder pathStr = new StringBuilder();
                Long current = j;
                while (current != null) {
                    pathStr.insert(0, current + " ");
                    current = cameFrom.get(current);
                }
                msg += " Computed path: " + pathStr.toString().trim();}
            throw new IllegalArgumentException(msg);
        }
        return cost;
    }

    /**
     * Return the outgoing neighbors for node {@code i} in this Graph.
     *
     * @param i node id
     * @return a non-null Set of neighbor ids; empty when no neighbors are present
     *
     * Note: the returned set is the actual set stored in the adjacency map and
     * modifying it will change this Graph's adjacency. Treat it as read-only.
     */
    public Set<Long> getNeighbors(Long i) {
        return adjacency.getOrDefault(i, Collections.emptySet());
    }

    private float heuristic(Long i, Long j) {
        double weight = 20.0;
        Node nodeI = all_nodes.get(i);
        Node nodeJ = all_nodes.get(j);
        // Current heuristic : euclidian distance
        return (float) (weight * Math.sqrt(Math.pow(nodeI.getLatitude() - nodeJ.getLatitude(), 2) + Math.pow(nodeI.getLongitude() - nodeJ.getLongitude(), 2)));
    }

    private void printd(String s) {
        boolean debug = true;
        if (debug)
            System.out.println(s);
    }

    /**
     * Run a weighted A* search from {@code startId} to {@code endId} using
     * this Graph's adjacency and cost information.
     *
     * <p>This method updates {@link #pathCost} for the (startId,endId) pair
     * with the computed Float cost when a path is found, or with {@code null}
     * when no path exists. The returned map maps each reachable node id to
     * its predecessor id and can be used to reconstruct the path.
     *
     * @param startId start node id
     * @param endId end node id
     * @return a predecessor map (node -> predecessor) for reachable nodes, or
     *         {@code null} when startId equals endId or the search early-exits
     *         because a node has no neighbors
     */
    public Map<Long, Long> AWAStar(Long startId, Long endId) {

        if (startId.equals(endId)){
            pathCost.put(new Pair<Long, Long>(startId, endId), 0f);
            return null;
        }

        if(getNeighbors(startId).isEmpty() || getNeighbors(endId).isEmpty()){
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
            NodeWithCost current = q.poll();

            if (visited.contains(current.getId()))
                continue;

            if(current.getId() == endId){
                pathCost.put(new Pair<Long, Long>(startId, endId), costMap.get(endId));
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

        // Check if we reached the end node
        if (visited.contains(endId)) {
            pathCost.put(new Pair<Long, Long>(startId, endId), costMap.get(endId));
        } else {
            pathCost.put(new Pair<Long, Long>(startId, endId), null);
            printd("No path found from " + startId + " to " + endId);
        }
        return cameFrom;
    }

    //=========================== Getters ==================================
    /** @return list of all edges (may be null if not set) */
    public List<Edge> getAllEdges() {
        return all_edges;
    }

    /** @return map of node id -> Node (may be null if not set) */
    public Map<Long, Node> getAllNodes() {
        return all_nodes;
    }

    /**
     * @return map of direct edge costs (pair -> cost); entries absent when no
     *         direct edge
     */
    public Map<Pair<Long, Long>, Float> getAllCosts() {
        return all_costs;
    }

    /** @return tour map of PoIs (id -> PointOfInterest) */
    public List<PointOfInterest> getTour() {
        return tour;
    }

    /** @return cached path costs computed by AWAStar */
    public Map<Pair<Long, Long>, Float> getPathCostMap() {
        return pathCost;
    }

    /** @return adjacency map (id -> set of neighbor ids) */
    public Map<Long, Set<Long>> getAdjacency() {
        return adjacency;
    }

}
