package backend.TSP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import backend.models.Graph;

public abstract class TemplateTSP implements TSP {
	
	private Long[] meilleureSolution;
	protected Graph g;
	private double coutMeilleureSolution;
	private int tpsLimite;
	private long tpsDebut;
	
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
		branchAndBound(g.getBeginId(), nonVus, vus, g.getBeginId());
	}
	
	public long getSolution(int i){
		if (g != null && i>=0 && i<g.getNbNodes())
			return meilleureSolution[i];
		return -1;
	}
	
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
	protected abstract double bound(Long sommetCourant, Collection<Long> nonVus);
	
	/**
	 * Methode devant etre redefinie par les sous-classes de TemplateTSP
	 * @param sommetCrt
	 * @param nonVus
	 * @param g
	 * @return un iterateur permettant d'iterer sur tous les sommets de <code>nonVus</code> qui sont successeurs de <code>sommetCourant</code>
	 */
	protected abstract Iterator<Long> iterator(Long sommetCrt, Collection<Long> nonVus, Graph g);
	
	/**
	 * Methode definissant le patron (template) d'une resolution par separation et evaluation (branch and bound) du TSP pour le graphe <code>g</code>.
	 * @param sommetCrt le dernier sommet visite
	 * @param nonVus la liste des sommets qui n'ont pas encore ete visites
	 * @param vus la liste des sommets deja visites (y compris sommetCrt)
	 * @param coutVus la somme des couts des arcs du chemin passant par tous les sommets de vus dans l'ordre ou ils ont ete visites
	 */	
	private void branchAndBound(long sommetCrt, Collection<Long> nonVus, Collection<Long> vus, double coutVus){

		if (System.currentTimeMillis() - tpsDebut > tpsLimite) return;
	    if (nonVus.size() == 0){ // tous les sommets ont ete visites
			
	    	if (g.isEdge(sommetCrt,g.getBeginId())){ // on peut retourner au sommet de depart
				
			
	    		if (coutVus+g.getCost(sommetCrt,g.getBeginId()) < coutMeilleureSolution){ // on a trouve une solution meilleure que meilleureSolution
	    			
					vus.toArray(meilleureSolution);
	    			coutMeilleureSolution = coutVus+g.getCost(sommetCrt,g.getBeginId());
	    		}
	    	}
	    } else if (coutVus+bound(sommetCrt,nonVus) < coutMeilleureSolution){
	        Iterator<Long> it = iterator(sommetCrt, nonVus, g);
	        while (it.hasNext()){
	        	Long prochainSommet = it.next();
				
	        	vus.add(prochainSommet);
				if (g.getDelivery(prochainSommet) != null) {
					nonVus.add(g.getDelivery(prochainSommet));
				}
	            nonVus.remove(prochainSommet);
	            branchAndBound(prochainSommet, nonVus, vus, coutVus+g.getCost(sommetCrt, prochainSommet));
	            vus.remove(prochainSommet);
	            nonVus.add(prochainSommet);
				if (g.getDelivery(prochainSommet) != null) {
					nonVus.remove(g.getDelivery(prochainSommet));
				}
	        }	    
	    }
	}
}

