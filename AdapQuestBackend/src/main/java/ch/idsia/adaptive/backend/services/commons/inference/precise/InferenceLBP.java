package ch.idsia.adaptive.backend.services.commons.inference.precise;

import ch.idsia.adaptive.backend.services.commons.inference.InferenceEngine;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.bp.LoopyBeliefPropagation;
import ch.idsia.crema.model.graphical.DAGModel;
import gnu.trove.map.TIntIntMap;

import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    21.05.2021 09:31
 */
public class InferenceLBP implements InferenceEngine<BayesianFactor> {

	@Override
	public BayesianFactor query(DAGModel<BayesianFactor> model, TIntIntMap obs, int variable) {
		final LoopyBeliefPropagation<BayesianFactor> inference = new LoopyBeliefPropagation<>();
		return inference.query(model, obs, variable);
	}

	@Override
	public List<BayesianFactor> query(DAGModel<BayesianFactor> model, TIntIntMap obs, int... variable) {
		final LoopyBeliefPropagation<BayesianFactor> inference = new LoopyBeliefPropagation<>();
		return inference.query(model, obs, variable);
	}
}
