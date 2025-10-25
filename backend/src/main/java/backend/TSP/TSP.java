package backend.TSP;

import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;

import backend.models.Pair;

//import backend.models.Node;

public interface TSP {

	/**
	 * Cherche une solution au TSP pour le graphe <code>g</code> dans la limite de
	 * <code>tpsLimite</code> millisecondes
	 * Attention : la solution calculee commence necessairement par le sommet 0
	 */
	public void chercheSolution();

	/**
	 * @return la somme des couts des arcs de la solution calculee par
	 *         <code>chercheSolution</code>
	 *         (-1 si <code>chercheSolution</code> n'a pas encore ete appele).
	 */
	public double getCoutSolution();

	/**
	 * Return the full solution found by the last search as an ordered list of node
	 * ids
	 * 
	 * @return the solution ordered as a LinkedList of node ids
	 */
	public LinkedList<Long> getSolutionOrder();

	/**
	 * Return the full solution found by the last search as an ordered set of maps
	 * of paths between points of interest.
	 *
	 * @return the solution ordered as a LinkedHashSet of maps of paths (from node
	 *         to node)
	 */
	public LinkedHashSet<Map<Pair<Long, Long>, LinkedList<Long>>> getSolutionPath();

	/**
	 * Return the full solution found by the last search as an ordered set of node
	 * ids.
	 * 
	 * @return the solution ordered as a LinkedHashSet of node ids with their time
	 *         of arrival
	 */
	public LinkedList<Pair<Long, LocalTime>> getSolutionOrderWithArrivalTime();
}
