package backend.TSP;


//import backend.models.Node;

public interface TSP {
	
	/**
	 * Cherche une solution au TSP pour le graphe <code>g</code> dans la limite de <code>tempsLimite</code> millisecondes
	 * Attention : la solution calculee commence necessairement par le sommet 0
	 * @param tempsLimite
	 * @param g
	 */
	public void chercheSolution(int tempsLimite, Graph g);
	
	/**
	 * @param i
	 * @return le ieme sommet visite dans la solution calculee par <code>chercheSolution</code> 
	 * (-1 si <code>chercheSolution</code> n'a pas encore ete appele, ou si i < 0 ou i >= g.getNbSommets())
	 */
	public long getSolution(int i);
	
	/** 
	 * @return la somme des couts des arcs de la solution calculee par <code>chercheSolution</code> 
	 * (-1 si <code>chercheSolution</code> n'a pas encore ete appele).
	 */
	public double getCoutSolution();
}
