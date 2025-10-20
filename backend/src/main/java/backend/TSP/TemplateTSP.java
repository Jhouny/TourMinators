package backend.TSP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.LinkedList;

import backend.models.Pair;
import backend.models.PointOfInterest;

/**
 * Template for solving a TSP instance using branch-and-bound.
 *
 * <p>This abstract class implements the generic search flow and leaves two
 * strategy points to subclasses: {@link #bound(Long, Collection)} and
 * {@link #iterator(Long, Collection, Graph)}. The methods and fields use a
 * {@link Graph} instance supplied to {@link #chercheSolution(int, Graph)}.
 *
 * <p>Notes on behavior:
 * - All public methods operate on the Graph instance stored in {@code g}.
 * - The search respects a time limit (milliseconds) passed to
 *   {@link #chercheSolution(int, Graph)}. When the limit is exceeded the
 *   search returns early.
 */
public abstract class TemplateTSP implements TSP {
	
	protected Graph g;
	private double coutMeilleureSolution;
	private int timeLimit;
	private long startTime;
	private LinkedList<Long> solutionOrder;  // Solution is an ordered set of node ids representing the order of visit
	private LinkedHashSet<Map<Pair<Long, Long>, LinkedList<Long>>> solutionPath; // Solution is an ordered set of paths (Pair from PoI node to PoI node) with an ordered list of nodes representing the full path between them
	
	public TemplateTSP(int timeLimit, Graph g) {
		this.timeLimit = timeLimit;
		this.startTime = 0;
		this.g = g;
		this.solutionOrder = new LinkedList<Long>();
		this.solutionPath = new LinkedHashSet<Map<Pair<Long, Long>, LinkedList<Long>>>();
	}

	/**
	 * Start the branch-and-bound search to find a TSP solution.
	 *
	 * @param timeLimit time limit in milliseconds; if <= 0 the method returns
	 *                  immediately without searching
	 * @param g         the {@link Graph} to solve; stored in this instance
	 *
	 * Side effects: populates {@link #solutionOrder} and
	 * {@link #coutMeilleureSolution} when a solution is found. The method
	 * returns early if the time limit elapses.
	 */
	public void chercheSolution(){
		if (timeLimit <= 0) return;
		this.startTime = System.currentTimeMillis();

		Collection<Long> nonVus = g.getPickupPoIs();
		ArrayList<Long> vus = new ArrayList<Long>(g.getNbNodes());
		vus.add(g.getBeginId()); // le premier sommet visite
		coutMeilleureSolution = Double.MAX_VALUE;
		branchAndBound(g.getBeginId(), nonVus, vus, 0.0);

		// Calculate the nodes to return to the warehouse at the end if not already present
		if (solutionOrder.size() > 0 && solutionOrder.getLast() != g.getBeginId()) {
			Long previous = solutionOrder.getLast();
			Long last = previous;

			Map<Long, Long> cameFrom = g.AWAStar(solutionOrder.getLast(), g.getBeginId());
			Long current = g.getBeginId();
			LinkedList<Long> pathToWarehouse = new LinkedList<>();
			while (current != null) {
				pathToWarehouse.addFirst(current);
				previous = current;
				current = cameFrom.get(current);
			}
			
			// Remove the first node as it is the last node of the current solution
			pathToWarehouse.removeFirst();
			previous = last;
			for (Long nodeId : pathToWarehouse) {
				solutionOrder.add(nodeId);
				// Update weight
				coutMeilleureSolution += g.getPathCost(previous, nodeId);
				previous = nodeId;
			}

			Map<Pair<Long, Long>, LinkedList<Long>> pathMap = Map.of(new Pair<>(last, g.getBeginId()), pathToWarehouse);
			solutionPath.add(pathMap);
		}

	}
	
	/**
	 * Return the full solution found by the last search as an ordered set of node ids.
	 * 
	 * @return the solution ordered as a LinkedHashSet of node ids 
	 */
	public LinkedList<Long> getSolutionOrder() {
		return solutionOrder;
	}
	

	/**
	 * Return the full solution found by the last search as an ordered set of maps of paths between points of interest.
	 *
	 * @return the solution ordered as a LinkedHashSet of maps of paths (from node to node)
	 */
	public LinkedHashSet<Map<Pair<Long, Long>, LinkedList<Long>>> getSolutionPath() {
		return solutionPath;
	}

	/**
	 * Return the cost of the best solution found by the last search.
	 *
	 * @return the cost as a double, or -1 when no graph is set
	 */
	public double getCoutSolution(){
		if (g != null)
			return coutMeilleureSolution;
		return -1;
	}
	
	/**
	 * Validate the solution order
	 * @param List<Long> order
	 * @return true if the solution is valid, false otherwise
	 * 
	 */
	public boolean validateIncompleteOrder(List<Long> order) {
		// Verify that the first node is the warehouse
		if (order.get(0) != g.getBeginId()) 
			return false;

		// Verify that Pickups come before their associated Deliveries
		LinkedList<Long> check = new LinkedList<Long>();
		
		for (Long id : order) {
			PointOfInterest.PoIEnum type = g.getTypePoI(id);
			if (type == PointOfInterest.PoIEnum.PICKUP) {
				check.add(g.getAssociatedPoI(id));
			} else if (type == PointOfInterest.PoIEnum.DELIVERY) {
				if (!check.remove(id)) {
					return false;
				}
			}
		}

		return true;

	}


	/**
	 * Methode devant etre redefinie par les sous-classes de TemplateTSP
	 * @param sommetCourant
	 * @param nonVus
	 * @return une borne inferieure du cout des chemins de <code>g</code> partant de <code>sommetCourant</code>, visitant 
	 * tous les sommets de <code>nonVus</code> exactement une fois, puis retournant sur le sommet de départ.
	 */
	/**
	 * Compute a lower bound for the cost of completing the tour.
	 *
	 * @param sommetCourant current node id
	 * @param nonVus        collection of not-yet-visited node ids
	 * @return a lower bound (minimum possible additional cost) for paths in
	 *         the current graph that start at {@code sommetCourant}, visit all
	 *         {@code nonVus} exactly once and return to the start
	 */
	protected abstract double bound(Long sommetCourant, Collection<Long> nonVus);
	
	/**
	 * Provide an iterator over candidate successors of {@code sommetCrt}.
	 *
	 * @param sommetCrt current node id
	 * @param nonVus    collection of nodes not yet visited
	 * @param g         the Graph instance (same as this.g)
	 * @return an Iterator over node ids from {@code nonVus} that are valid
	 *         successors of {@code sommetCrt}. Implementations should return
	 *         an empty iterator rather than null when there are no candidates.
	 */
	protected abstract Iterator<Long> iterator(Long sommetCrt, Collection<Long> nonVus, Graph g);
	
	/**
	 * Methode definissant le patron (template) d'une resolution par separation et evaluation (branch and bound) du TSP pour le graphe <code>g</code>.
	 * @param sommetCrt le dernier sommet visite
	 * @param nonVus la liste des sommets qui n'ont pas encore ete visites
	 * @param vus la liste des sommets deja visites (y compris sommetCrt)
	 * @param coutVus la somme des couts des arcs du chemin passant par tous les sommets de vus dans l'ordre ou ils ont ete visites
	 */	
	/**
	 * Core branch-and-bound recursive procedure.
	 *
	 * <p>Updates {@link #meilleureSolution} and {@link #coutMeilleureSolution}
	 * when a better full tour is found. The method respects the time limit
	 * imposed to {@link #TemplateTSP} and returns early when
	 * the limit elapses.
	 *
	 * @param sommetCrt current (last visited) node id
	 * @param nonVus    collection of nodes not yet visited
	 * @param vus       collection of nodes already visited (contains sommetCrt)
	 * @param coutVus   cumulative cost of the path visiting nodes in {@code vus}; must start as 0.0
	 *
	 * @throws IllegalArgumentException when an encountered PoI is associated to
	 *         itself (this is considered an invalid input)
	 * @throws NullPointerException when the Graph or required PoI data is missing
	 */
	private void branchAndBound(Long sommetCrt, Collection<Long> nonVus, ArrayList<Long> vus, double coutVus) {
		//if ( System.currentTimeMillis() - startTime > timeLimit )
			//return;

		// If all nodes have been visited, check if we can return to start
		if ( nonVus.isEmpty() ) {
			if ( g.pathCost.containsKey(new Pair<>(sommetCrt, g.getBeginId())) || sommetCrt == g.getBeginId() ) {
				// Check if this solution is better than the best one so far
				if ( coutVus < coutMeilleureSolution ) {
					coutMeilleureSolution = coutVus;
					solutionOrder.clear();
					for (Long l : vus) {
						solutionOrder.add(l);
					}
					
					solutionPath.clear();
					Long previous = null;
					Map<Pair<Long, Long>, LinkedList<Long>> pathMap = null;
					for (Long l : vus) {
						if (previous != null) {
							LinkedList<Long> path = new LinkedList<>();
							path.add(previous);
							Map<Long, Long> cameFrom = g.AWAStar(previous, l);
							Long current = l;
							for (Long key : cameFrom.keySet()) {
								if (cameFrom.get(key) == null) {
									continue;
								}

								path.addFirst(key);
							}
							pathMap = Map.of(new Pair<>(previous, l), path);
							solutionPath.add(pathMap);
						}
						previous = l;
					}
				}
			}
		} else if ( coutVus + bound(sommetCrt, nonVus) < coutMeilleureSolution ) { // If there is potential for a better solution
			for ( Long neighbour : g.getNeighbors(sommetCrt) ) {
				Long prochainSommet = neighbour;
				boolean wasInNonVus = nonVus.contains(prochainSommet);

				vus.add(prochainSommet);
                
				Long assoc = g.getAssociatedPoI(prochainSommet);
				if ( g.getTypePoI(prochainSommet) == PointOfInterest.PoIEnum.PICKUP ) {
					// Pickup is being visited, add associated delivery to nonVus
					if (!nonVus.contains(assoc))
						nonVus.add(assoc);
				}


				nonVus.remove(prochainSommet);
				branchAndBound(prochainSommet, nonVus, vus, coutVus+g.getPathCost(sommetCrt, prochainSommet));
				vus.remove(vus.lastIndexOf(prochainSommet));
				if ( wasInNonVus )
					nonVus.add(prochainSommet);

				// Remove associated delivery if we just backtracked from a pickup
				if ( assoc != null) 
					nonVus.remove(assoc);
			}        
		}
	}
}

