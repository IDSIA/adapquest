package ch.idsia.adaptive.backend.services.commons.scoring.precise;

import ch.idsia.adaptive.backend.services.commons.scoring.Scoring;
import ch.idsia.crema.entropy.BayesianEntropy;
import ch.idsia.crema.factor.bayesian.BayesianFactor;


/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    05.02.2021 15:49
 */
public class ScoringFunctionExpectedEntropy implements Scoring<BayesianFactor> {

	/**
	 * A {@link Scoring} based on the expected mean entropy change from the answer.
	 *
	 * @param factor computer factor for the given combination of question, skill and answer
	 * @return the score, between 0 and 1, associated with the given factor
	 */
	@Override
	public double score(BayesianFactor factor) {
		return BayesianEntropy.H(factor);
	}

}
