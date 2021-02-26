package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Survey;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:18
 */
public class NonAdaptiveSurvey extends AbstractSurvey {

	public NonAdaptiveSurvey(Survey model, Long seed) {
		super(model, seed);
	}

	@Override
	public Question findNext() {
		Question nextQuestion;
		if (survey.getQuestionsAreRandom()) {
			int i = random.nextInt(questions.size());
			nextQuestion = questions.remove(i);
		} else {
			nextQuestion = questions.poll();
		}

		return nextQuestion;
	}

	@Override
	public boolean isFinished() {
		return questions.isEmpty();
	}
}
