package tsp_pnd;

public interface Graphe {

	/**
	 * @return le nombre de sommets de <code>this</code>
	 */
	public abstract int getNbSommets();

	/**
	 * @param i 
	 * @param j 
	 * @return le cout de l'arc (i,j) si (i,j) est un arc ; -1 sinon
	 */
	public abstract int getCout(Long i, Long j);
	
	/**
	 * @param i 
	 * @param j 
	 * @return true si <code>(i,j)</code> est un arc de <code>this</code>
	 */
	public abstract boolean estArc(long i, long j);


}