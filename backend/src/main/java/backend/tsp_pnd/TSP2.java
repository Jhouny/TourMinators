package tsp_pnd;

import java.util.Collection;
import java.util.Iterator;

public class TSP2 extends TemplateTSP {


	@Override
	protected double bound(Long sommetCourant, Collection<Long> nonVus) {
		// on veut une borne inferieure la plus haute possible du cout des chemins passant par tous les sommets de nonVus

		 // il n'y a plus de sommets a visiter : on prend la longeur pour arriver au sommet de depart
		if (nonVus.size() == 0) return g.getCout(sommetCourant,g.getBeginId());
		else {
			// on prend la longeur pour arriver au sommet 0 en passant par le sommet le plus loin des sommets restants
			double max = Double.MIN_VALUE;
			for (Long i : nonVus){
				if (g.getCout(sommetCourant,i) + g.getCout(i,g.getBeginId()) > max)
					max = g.getCout(sommetCourant,i) + g.getCout(i,g.getBeginId());
			}
			return max;
		}
	}

	@Override
	protected Iterator<Long> iterator(Long sommetCrt, Collection<Long> nonVus, Graphe g) {
		return new IteratorSeq(nonVus, sommetCrt, g);
	}

}
