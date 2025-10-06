package tsp;

import java.util.Collection;
import java.util.Iterator;

public class TSP2 extends TemplateTSP {


	@Override
	protected int bound(Integer sommetCourant, Collection<Integer> nonVus) {
		// on veut une borne inferieure la plus haute possible du cout des chemins passant par tous les sommets de nonVus
		//todo : récupérer g
		//Graphe g = this.g;

		 // on prend la longeur pour arriver au sommet 0 en passant par le sommet le plus loin des sommets restants
		if (nonVus.size() == 0) return g.getCout(sommetCourant,0);
		else {
			// on prend la longeur pour arriver au sommet 0 en passant par le sommet le plus loin des sommets restants
			int max = Integer.MIN_VALUE;
			for (Integer i : nonVus){
				if (g.getCout(sommetCourant,i) + g.getCout(i,0) > max)
					max = g.getCout(sommetCourant,i) + g.getCout(i,0);
			}
			return max;
		}
	}

	@Override
	protected Iterator<Integer> iterator(Integer sommetCrt, Collection<Integer> nonVus, Graphe g) {
		return new IteratorSeq(nonVus, sommetCrt, g);
	}

}
