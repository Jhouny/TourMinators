package backend.models;

import java.util.ArrayList;

public interface Graph {

	/**
	 * @return le nombre de sommets de <code>this</code>
	 */
	public abstract int getNbNodes();

	/**
	 * @param i 
	 * @param j 
	 * @return le cout de l'arc (i,j) si (i,j) est un arc ; -1 sinon
	 */
	public abstract float getCost(long i, long j);
	
	/**
	 * @param i 
	 * @param j 
	 * @return true si <code>(i,j)</code> est un arc de <code>this</code>
	 */
	public abstract boolean isEdge(long i, long j);
	
	/**
	 * @return la liste des identifiants des sommets a visiter (avec le sommet de depart)
	 */
	public abstract ArrayList<Long> getNodesToVisit();

	/**
	 * @return l'id du sommet de depart)
	 */
	public abstract Long getBeginId();

	/**
	 * @param id id du noeud
	 * @return null si id est un delivery, l'id de son delivery si id est un pickup
	 */
	public abstract Long getDelivery(Long id);


}