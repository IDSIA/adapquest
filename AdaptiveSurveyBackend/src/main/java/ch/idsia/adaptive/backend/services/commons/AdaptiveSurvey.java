package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.AdaptiveModel;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:17
 */
public class AdaptiveSurvey extends AbstractSurvey {

	public AdaptiveSurvey(AdaptiveModel model) {
		super(model);
	}

	@Override
	public void check() {
		// TODO
		throw new NotImplementedException();
	}

	@Override
	public void next() {
		// TODO
		throw new NotImplementedException();
	}
}
