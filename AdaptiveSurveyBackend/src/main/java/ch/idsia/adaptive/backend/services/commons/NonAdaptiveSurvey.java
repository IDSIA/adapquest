package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Survey;

import java.util.LinkedList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:18
 */
public class NonAdaptiveSurvey extends AbstractSurvey {

	protected LinkedList<Question> questions = new LinkedList<>();

	public NonAdaptiveSurvey(Survey model, Long seed) {
		super(model, seed);
	}

	@Override
	public void addQuestions(List<Question> questions) {
		this.questions.addAll(questions);
	}

	@Override
	public void check(Answer answer) {
		// nothing
	}

	@Override
	public Question next() {
		if (survey.getQuestionsAreRandom()) {
			int i = random.nextInt(questions.size());
			return questions.remove(i);
		} else {
			return questions.poll();
		}
	}

	@Override
	public boolean isFinished() {
		return questions.isEmpty();
	}
}
