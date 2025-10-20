package backend.TSP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import backend.TSP.Graph;
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
	
	private Long[] meilleureSolution;
	protected Graph g;
	private double coutMeilleureSolution;
	private int tpsLimite;
	private long tpsDebut;
	
	/**
	 * Start the branch-and-bound search to find a TSP solution.
	 *
	 * @param tpsLimite time limit in milliseconds; if <= 0 the method returns
	 *                  immediately without searching
	 * @param g         the {@link Graph} to solve; stored in this instance
	 *
	 * Side effects: populates {@link #meilleureSolution} and
	 * {@link #coutMeilleureSolution} when a solution is found. The method
	 * returns early if the time limit elapses.
	 */
	public void chercheSolution(int tpsLimite, Graph g){
		if (tpsLimite <= 0) return;
		tpsDebut = System.currentTimeMillis();
		this.tpsLimite = tpsLimite;
		this.g = g;
		meilleureSolution = new Long[g.getNbNodes()];
		Collection<Long> nonVus = g.getNodesToVisit();
		Collection<Long> vus = new ArrayList<Long>(g.getNbNodes());
		vus.add(g.getBeginId()); // le premier sommet visite
		coutMeilleureSolution = Double.MAX_VALUE;
		System.out.println("\n================================\n");
		System.out.println(nonVus);
		System.out.println("\n================================\n");
		branchAndBound(g.getBeginId(), nonVus, vus, 0.0); 
		// Bug: C'etait g.getBeginId() pour le cout au lieu de 0.0
	}
	
	/**
	 * Return the node id at position {@code i} in the best solution found.
	 *
	 * @param i index (0-based)
	 * @return node id when available; otherwise -1 if the graph is not set or
	 *         index is out of range
	 */
	public long getSolution(int i){
		if (g != null && i>=0 && i<g.getNbNodes())
			return meilleureSolution[i];
		return -1;
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
	 * Methode devant etre redefinie par les sous-classes de TemplateTSP
	 * @param sommetCrt
	 * @param nonVus
	 * @param g
	 * @return un iterateur permettant d'iterer sur tous les sommets de <code>nonVus</code> qui sont successeurs de <code>sommetCourant</code>
	 */
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
	 * supplied to {@link #chercheSolution(int, Graph)} and returns early when
	 * the limit elapses.
	 *
	 * @param sommetCrt current (last visited) node id
	 * @param nonVus    collection of nodes not yet visited
	 * @param vus       collection of nodes already visited (contains sommetCrt)
	 * @param coutVus   cumulative cost of the path visiting nodes in {@code vus}
	 *
	 * @throws IllegalArgumentException when an encountered PoI is associated to
	 *         itself (this is considered an invalid input)
	 * @throws NullPointerException when the Graph or required PoI data is missing
	 */
	private void branchAndBound(long sommetCrt, Collection<Long> nonVus, Collection<Long> vus, double coutVus){
		if (System.currentTimeMillis() - tpsDebut > tpsLimite) return;
		System.out.println("branchAndBound called with sommetCrt: "+sommetCrt+", nonVus: "+nonVus+", vus: "+vus+", coutVus: "+coutVus+", coutMeilleureSolution: "+coutMeilleureSolution);
		if (nonVus.isEmpty()){ // tous les sommets ont ete visites
			if (g.isEdge(sommetCrt,g.getBeginId())){ // on peut retourner au sommet de depart
				if (coutVus+g.getPathCost(sommetCrt,g.getBeginId()) < coutMeilleureSolution){ // on a trouve une solution meilleure que meilleureSolution
					vus.toArray(meilleureSolution);
					coutMeilleureSolution = coutVus+g.getPathCost(sommetCrt,g.getBeginId());
				}
			}
		} 
		else if (coutVus+bound(sommetCrt,nonVus) < coutMeilleureSolution){
			Iterator<Long> it = iterator(sommetCrt, nonVus, g);
			while (it.hasNext()){
				Long prochainSommet = it.next();
                
				vus.add(prochainSommet);
                
				Long assoc = g.getAssociatedPoI(prochainSommet);
				if (g.getTypePoI(prochainSommet) == PointOfInterest.PoIEnum.PICKUP) {
					if (assoc.equals(prochainSommet)){
						System.out.println("Error: PoI associated to itself: " + prochainSommet);
						throw new IllegalArgumentException("PoI associated to itself: " + prochainSommet);
					}
					System.out.println(assoc+" "+ prochainSommet);

					if (!vus.contains(assoc) && !nonVus.contains(assoc)) {
					nonVus.add(assoc);
					}
				}

				nonVus.remove(prochainSommet);
				branchAndBound(prochainSommet, nonVus, vus, coutVus+g.getPathCost(sommetCrt, prochainSommet));
				vus.remove(prochainSommet);
				nonVus.add(prochainSommet);
				if (g.getAssociatedPoI(prochainSommet).equals(prochainSommet)){
					System.out.println("Error: PoI associated to itself: " + prochainSommet);
					throw new IllegalArgumentException("PoI associated to itself: " + prochainSommet);
				}
				if (g.getAssociatedPoI(prochainSommet) != null) {
					nonVus.remove(g.getAssociatedPoI(prochainSommet));
				}
			}        
		}
	}
}

