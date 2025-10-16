package backend.TSP;

import java.util.Collection;
import java.util.Iterator;

import backend.TSP.Graph;

public class TSP2 extends TemplateTSP {


	@Override
	protected double bound(Long sommetCourant, Collection<Long> nonVus) {
		// on veut une borne inferieure la plus haute possible du cout des chemins passant par tous les sommets de nonVus

		 // il n'y a plus de sommets a visiter : on prend la longeur pour arriver au sommet de depart
		if (nonVus.size() == 0) return g.getCost(sommetCourant,g.getBeginId());
		else {
			// on prend la longeur pour arriver au sommet 0 en passant par le sommet le plus loin des sommets restants
			double max = Double.MIN_VALUE;
			for (Long i : nonVus){
				if (g.getCost(sommetCourant,i) + g.getCost(i,g.getBeginId()) > max)
					max = g.getCost(sommetCourant,i) + g.getCost(i,g.getBeginId());
			}
			return max;
		}
	}

	@Override
	protected Iterator<Long> iterator(Long sommetCrt, Collection<Long> nonVus, Graph g) {
		return new IteratorSeq(sommetCrt, nonVus , g);
	}

}
