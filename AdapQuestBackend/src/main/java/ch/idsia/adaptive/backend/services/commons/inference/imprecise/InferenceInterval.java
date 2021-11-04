package ch.idsia.adaptive.backend.services.commons.inference.imprecise;

import ch.idsia.adaptive.backend.services.commons.inference.InferenceEngine;
import ch.idsia.crema.factor.credal.linear.interval.IntervalFactor;
import ch.idsia.crema.inference.approxlp1.ApproxLP1;
import ch.idsia.crema.model.graphical.DAGModel;
import gnu.trove.map.TIntIntMap;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    23.06.2021 15:34
 */
public class InferenceInterval implements InferenceEngine<IntervalFactor> {

	@Override
	public IntervalFactor query(DAGModel<IntervalFactor> model, TIntIntMap obs, int variable) {
		final ApproxLP1<IntervalFactor> alp1 = new ApproxLP1<>();
		return alp1.query(model, obs, variable);
	}

}
