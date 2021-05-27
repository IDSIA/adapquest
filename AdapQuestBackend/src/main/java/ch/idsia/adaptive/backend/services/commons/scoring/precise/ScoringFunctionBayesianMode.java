package ch.idsia.adaptive.backend.services.commons.scoring.precise;

import ch.idsia.adaptive.backend.services.commons.scoring.Scoring;
import ch.idsia.crema.factor.bayesian.BayesianFactor;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    08.02.2021 15:23
 */
public class ScoringFunctionBayesianMode implements Scoring<BayesianFactor> {

	/**
	 * A {@link Scoring} based on the mode of the probability to answer correctly.
	 *
	 * @param factor computer factor for the given combination of question, skill and answer
	 * @return the score, between 0 and 1, associated with the given factor
	 */
	@Override
	public double score(BayesianFactor factor) {
		final double[] data = factor.getData();

		double argValue = data[0];
		int argMax = 0;

		for (int i = 1; i < data.length; i++) {
			if (data[i] > argValue) {
				argValue = data[i];
				argMax = i;
			}
		}

		return argMax;
	}

}
