package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.dao.AnswerRepository;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.services.commons.AbstractSurvey;
import ch.idsia.adaptive.backend.services.commons.AdaptiveSurvey;
import ch.idsia.adaptive.backend.services.commons.NonAdaptiveSurvey;
import org.apache.commons.lang3.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    03.12.2020 09:48
 */
@Service
public class SurveyManagerService {

	private final SurveyRepository surveyRepository;
	private final AnswerRepository answerRepository;

	// TODO: add cache for models
	private final Map<String, AbstractSurvey> activeSurveys = new HashMap<>();

	@Autowired
	public SurveyManagerService(SurveyRepository surveyRepository, AnswerRepository answerRepository) {
		this.surveyRepository = surveyRepository;
		this.answerRepository = answerRepository;
	}

	/**
	 * Load the {@link Survey} associated with the active session stored in the given {@link SurveyData}.
	 *
	 * @param data the {@link SurveyData} passed must be initialized correctly from a {@link SessionService}.
	 * @throws IllegalArgumentException when the survey id is not valid
	 */
	public void init(SurveyData data) {
		Long surveyId = data.getSurveyId();
		Survey survey = surveyRepository
				.findById(surveyId)
				.orElseThrow(() -> new IllegalArgumentException("No model associated with SurveyId=" + surveyId));

		AbstractSurvey content;

		if (survey.getIsAdaptive()) {
			content = new AdaptiveSurvey(survey);
		} else {
			content = new NonAdaptiveSurvey(survey);
		}

		content.addQuestions(survey.getQuestions());

		activeSurveys.put(data.getToken(), content);
	}

	public AbstractSurvey getSurvey(SurveyData data) {
		String token = data.getToken();
		return Optional.ofNullable(activeSurveys.get(token))
				.orElseThrow(() -> new IllegalArgumentException("Cannot load status: no model for token=" + token));
	}

	public Status getState(SurveyData data) {
		return getSurvey(data).getState();
	}

	public boolean isFinished(SurveyData data) {
		return getSurvey(data).isFinished();
	}

	public void checkAnswer(SurveyData data, Answer answer) {
		answerRepository.save(answer);
		getSurvey(data).check(answer);
	}

	public Question nextQuestion(SurveyData data) {
		return getSurvey(data).next();
	}

	public void complete(SurveyData data) {
		// TODO:
		//  - remove inference and model from map
		//  - collect results
		throw new NotImplementedException();
	}
}
