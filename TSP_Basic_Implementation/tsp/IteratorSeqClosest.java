package tsp;

import java.util.Collection;
import java.util.Iterator;

public class IteratorSeqClosest implements Iterator<Integer> {

	private Integer[] candidats;
	private int nbCandidats;
	private Graphe g;

	/**
	 * Cree un iterateur pour iterer sur l'ensemble des sommets de nonVus qui sont successeurs de sommetCrt dans le graphe g,
	 * dans l'odre d'apparition dans <code>nonVus</code>
	 * @param nonVus
	 * @param sommetCrt
	 * @param g
	 */
	public IteratorSeqClosest(Collection<Integer> nonVus, int sommetCrt, Graphe g){
		this.candidats = new Integer[nonVus.size()];
		this.g = g;
		Iterator<Integer> it = nonVus.iterator();
		while (it.hasNext()){
			Integer s = it.next();
			if (g.estArc(sommetCrt, s))
				candidats[nbCandidats++] = s;
		}
	}
	
	@Override
	public boolean hasNext() {
		return nbCandidats > 0;
	}

	@Override
	public Integer next() {
		int minLength = Integer.MAX_VALUE;
		int minIndex = -1;
		for (int i=0; i<nbCandidats; i++){
			if (g.getCout(candidats[i],0) < minLength){
				minLength = g.getCout(candidats[i],0);
				minIndex = i;
			}
		}
		int closestCandidate = candidats[minIndex];
		for (int i=minIndex; i<nbCandidats-1; i++){
			candidats[i] = candidats[i+1];
		}
		nbCandidats--;
		return closestCandidate;
	}

	@Override
	public void remove() {}
}
