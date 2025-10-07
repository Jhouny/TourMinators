package tsp;

import java.io.ObjectInputFilter.Status;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import backend.models.Edge;
import backend.models.Node;


public class GrapheAWA implements Graphe {
	
    List<Edge> edges;
    Map<Long, Node> nodes;
    int nbNodes;

    //On doit utiliser des HashSets et HashMaps puisque les ids ne correspondent pas à 1-n mais sont aléatoires.
    //Cest impossible alors à les indexer qu'avec les ids.

    Map<Set<Long>, long> costs = new HashMap<>();
    Map<Long, Set<Long>> adjacency = new HashMap<>();
	
    // Crée un graphe incomplet à partir d'une Map de noeuds et d'une List d'arcs
	public GrapheAWA(Map<long, Node> nodes, List<Edge> edges){
        this.nodes = nodes; 
        this.edges = edges;
        this.nbNodes = nodes.size(); 

        // Build adjacency and cost tables
        for (Edge edge : edges) {
            long origin = edge.getOrigin();
            long destination = edge.getDestination();
            long distance = edge.getLength();

            // Build adjacency
            // computeIfAbsent is a lambda key function to add a Set to the adjancency if it doesnt exist
            adjacency.computeIfAbsent(origin, k -> new HashSet<>()).add(destination);
            adjacency.computeIfAbsent(destination, k -> new HashSet<>()).add(origin);

            // Store edge cost (unordered pair)
            Set<Long> pair = new HashSet<>(Arrays.asList(origin, destination));
            costs.put(pair, distance);
        }
	}
    @Override
	public int getNbSommets() {
		return nbNodes;
	}

    @Override
    public long getCout(long i, long j) {
        Set<Long> pair = new HashSet<>(Arrays.asList(i, j));
        if (costs.containsKey(pair)) {
            return costs.get(pair);
        } else {
            throw new IllegalArgumentException("No edge between " + i + " and " + j);
        }
    }

    @Override
    public boolean estArc(long i, long j) {
        return adjacency.containsKey(i) && adjacency.get(i).contains(j);
    }

    public Set<Long> getNeighbors(long i) {
        //GetOrDefault renvoie soit i si pas nul, soit un Set vide si i est nul
        return adjacency.getOrDefault(i, Collections.emptySet());
    }


    class NodeCost implements Comparable<NodeCost> {
        long id;
        double cost; // f = g + h

        NodeCost(long id, double cost) {
            this.id = id;
            this.cost = cost;
        }

        @Override
        public int compareTo(NodeCost other) {
            return Double.compare(this.cost, other.cost);
        }
    }


    public float heuristic(long i, long j) {
        Node nodeI = nodes.get(i);
        Node nodeJ = nodes.get(j);
        // Utilisation de la distance euclidienne comme heuristique
        return (int) Math.sqrt(Math.pow(nodeI.getLat() - nodeJ.getLat(), 2) + Math.pow(nodeI.getLong() - nodeJ.getLong(), 2));
    }

    public void printSolution(long startId, long endId, Map<Long, Float> costMap, Map<Long, Long> cameFrom) {
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
        PriorityQueue<NodeCost> q = new PriorityQueue<>();

        NodeCost startNode = new NodeCost(startId, heuristic(startId, endId));
        q.add(startNode);

        costMap.put(startId, 0f);
        cameFrom.put(startId, null);

        while(!q.isEmpty()){
            nbIter++;
            NodeCost current = q.poll();

            if (visited.contains(current.id))
                continue;
            
            if(current.id == endId){
                System.out.println("Nombre d'itérations : " + nbIter);
                printSolution(startId, endId, costMap, cameFrom);
                return 0;
            }

            for(long neighbor : getNeighbors(current.id)){
                float newCost = costMap.get(current.id) + getCout(current.id, neighbor);
                float h = heuristic(neighbor, endId);
                float f = newCost + h;

                if (!costMap.containsKey(neighbor) || newCost < costMap.get(neighbor)) {
                    costMap.put(neighbor, newCost);
                    cameFrom.put(neighbor, current.id);
                    q.add(new NodeCost(neighbor, f));
                }
            }

            visited.add(current.id);
        }

        return 0;
    }

}
