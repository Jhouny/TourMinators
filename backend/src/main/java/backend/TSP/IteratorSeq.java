package backend.TSP;

import java.util.Collection;
import java.util.Iterator;

public class IteratorSeq implements Iterator<Long> {

	private Long[] candidats;
	private int nbCandidats;

	/**
	 * Cree un iterateur pour iterer sur l'ensemble des sommets de nonVus qui sont successeurs de sommetCrt dans le graphe g,
	 * dans l'odre d'apparition dans <code>nonVus</code>
	 * @param nonVus
	 * @param sommetCrt
	 * @param g
	 */
	public IteratorSeq(Long sommetCrt, Collection<Long> nonVus , Graph g){
		this.candidats = new Long[nonVus.size()];
		Iterator<Long> it = nonVus.iterator();
		while (it.hasNext()){
			Long s = it.next();
			candidats[nbCandidats++] = s;
		}
	}
	
	@Override
	public boolean hasNext() {
		return nbCandidats > 0;
	}

	@Override
	public Long next() {
		nbCandidats--;
		return candidats[nbCandidats];
	}

	@Override
	public void remove() {}
}
