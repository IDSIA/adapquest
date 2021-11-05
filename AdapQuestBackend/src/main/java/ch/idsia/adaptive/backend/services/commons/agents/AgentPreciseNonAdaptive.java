package ch.idsia.adaptive.backend.services.commons.agents;

import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Survey;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    14.12.2020 17:18
 */
public class AgentPreciseNonAdaptive extends AgentPrecise {

	public AgentPreciseNonAdaptive(Survey model, Long seed) {
		super(model, seed, (f) -> 0);
		addSkills(survey.getSkills());
		addQuestions(survey.getQuestions());
	}

	@Override
	public Question nextQuestion() {
		Question nextQuestion;
		if (survey.getQuestionsAreRandom()) {
			questions.forEach(q -> q.setScore(random.nextDouble()));
		} else {
			for (int i = 0; i < questions.size(); i++) {
				questions.get(i).setScore(1.0 * questions.size() - i);
			}
		}

		double max = 0;
		nextQuestion = null;

		for (Question q : questions) {
			if (q.getScore() > max) {
				max = q.getScore();
				nextQuestion = q;
			}
		}

		return nextQuestion;
	}

	@Override
	public boolean checkStop() {
		return questions.isEmpty();
	}

}
