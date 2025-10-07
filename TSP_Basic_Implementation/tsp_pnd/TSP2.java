package tsp_pnd;

import java.util.Collection;
import java.util.Iterator;

public class TSP2 extends TemplateTSP {


	@Override
	protected int bound(Long sommetCourant, Collection<Long> nonVus) {
		// on veut une borne inferieure la plus haute possible du cout des chemins passant par tous les sommets de nonVus

		 // on prend la longeur pour arriver au sommet 0 en passant par le sommet le plus loin des sommets restants
		if (nonVus.size() == 0) return g.getCout(sommetCourant,0);
		else {
			// on prend la longeur pour arriver au sommet 0 en passant par le sommet le plus loin des sommets restants
			int max = Integer.MIN_VALUE;
			for (long i : nonVus){
				if (g.getCout(sommetCourant,i) + g.getCout(i,0) > max)
					max = g.getCout(sommetCourant,i) + g.getCout(i,0);
			}
			return max;
		}
	}

	@Override
	protected Iterator<Long> iterator(Long sommetCrt, Collection<Long> nonVus, Graphe g) {
		return new IteratorSeq_PnD(nonVus, sommetCrt, g);
	}

}
