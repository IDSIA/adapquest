package ch.idsia.adaptive.backend.services.commons.inference.imprecise;

import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.model.change.NullChange;
import ch.idsia.crema.model.graphical.DAGModel;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    14.04.2021 18:44
 */
// TODO: with crema 0.2.0 delete this class
public class CutObserved {

	public void executeInPlace(DAGModel<IntervalFactor> model, TIntIntMap evidence) {
		int size = evidence.size();

		TIntIntIterator iterator = evidence.iterator();
		for (int o = 0; o < size; ++o) {
			iterator.advance();
			final int observed = iterator.key();
			final int state = iterator.value();

			if (!model.getNetwork().containsVertex(observed))
				continue;

			for (int variable : model.getChildren(observed)) {
				model.removeParent(variable, observed, new NullChange<>() {
					@Override
					public IntervalFactor remove(IntervalFactor factor, int variable) {
						// probably need to check this earlier
						return factor.filter(observed, state);
					}
				});
			}
		}
	}
}

