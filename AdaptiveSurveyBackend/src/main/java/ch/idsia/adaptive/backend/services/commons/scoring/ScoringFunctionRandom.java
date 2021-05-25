package ch.idsia.adaptive.backend.services.commons.scoring;

import ch.idsia.crema.factor.GenericFactor;

import java.util.Random;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    09.02.2021 09:46
 */
public class ScoringFunctionRandom implements Scoring<GenericFactor> {

	final Random random;

	public ScoringFunctionRandom(long seed) {
		this.random = new Random(seed);
	}

	/**
	 * This scoring function can be used to propose answers in a pseudo-random order.
	 *
	 * @param factor ignored
	 * @return a random value between 0 and 1
	 */
	@Override
	public double score(GenericFactor factor) {
		return random.nextDouble();
	}

}
