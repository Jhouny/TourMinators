package backend.TSP;


//import backend.models.Node;

public interface TSP {
	
	/**
	 * Cherche une solution au TSP pour le graphe <code>g</code> dans la limite de <code>tpsLimite</code> millisecondes
	 * Attention : la solution calculee commence necessairement par le sommet 0
	 */
	public void chercheSolution();
	
	/** 
	 * @return la somme des couts des arcs de la solution calculee par <code>chercheSolution</code> 
	 * (-1 si <code>chercheSolution</code> n'a pas encore ete appele).
	 */
	public double getCoutSolution();
}
