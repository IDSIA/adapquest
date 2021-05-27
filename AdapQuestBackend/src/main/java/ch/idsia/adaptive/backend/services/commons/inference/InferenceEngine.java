package ch.idsia.adaptive.backend.services.commons.inference;

import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.model.graphical.DAGModel;
import gnu.trove.map.TIntIntMap;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    05.03.2021 15:34
 * <p>
 * This is for imprecise inferences.
 */
public interface InferenceEngine<F extends GenericFactor> {

	F query(DAGModel<F> model, TIntIntMap obs, int variable);

}
