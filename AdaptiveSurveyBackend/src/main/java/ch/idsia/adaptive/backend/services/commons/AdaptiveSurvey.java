package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import org.apache.commons.lang3.NotImplementedException;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:17
 */
public class AdaptiveSurvey extends AbstractSurvey {

	public AdaptiveSurvey(Survey model) {
		super(model);
	}

	@Override
	public void check(Answer answer) {
		// TODO
		throw new NotImplementedException();
	}

	@Override
	public Question next() {
		// TODO
		throw new NotImplementedException();
	}
}
