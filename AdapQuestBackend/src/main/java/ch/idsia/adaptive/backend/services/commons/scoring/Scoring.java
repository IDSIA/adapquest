package ch.idsia.adaptive.backend.services.commons.scoring;

import ch.idsia.crema.factor.GenericFactor;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    05.02.2021 15:33
 */
public interface Scoring<F extends GenericFactor> {

	/**
	 * @param factor computer factor for the given combination of question, skill and answer
	 * @return the score associated with the given factor
	 */
	double score(F factor);

}
