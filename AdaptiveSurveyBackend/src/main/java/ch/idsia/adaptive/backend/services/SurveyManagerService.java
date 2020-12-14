package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.dao.QuestionRepository;
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

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    03.12.2020 09:48
 */
@Service
public class SurveyManagerService {

	private final SurveyRepository surveyRepository;
	private final QuestionRepository questionRepository;

	// TODO: add cache for models
	private final Map<String, AbstractSurvey> activeSurveys = new HashMap<>();

	@Autowired
	public SurveyManagerService(SurveyRepository surveyRepository, QuestionRepository questionRepository) {
		this.surveyRepository = surveyRepository;
		this.questionRepository = questionRepository;
	}

	private Survey getSurvey(SurveyData data) {
		return surveyRepository.findById(data.getSurveyId())
				.orElseThrow(() -> new IllegalArgumentException("No model associated with SurveyId=" + data.getSurveyId()));
	}

	private AdaptiveModel getModel(SurveyData data) {
		return getSurvey(data).getModel();
	}

	/**
	 * Load the {@link AdaptiveModel} associated with the active session stored in the given {@link SurveyData}.
	 *
	 * @param data the {@link SurveyData} passed must be initialized correctly from a {@link SessionService}.
	 * @throws IllegalArgumentException when the survey id is not valid
	 */
	public void init(SurveyData data) {
		Survey survey = getSurvey(data);
		AdaptiveModel model = survey.getModel();
		AbstractSurvey content;

		if (model.getIsAdaptive()) {
			content = new AdaptiveSurvey(model);
		} else {
			content = new NonAdaptiveSurvey(model);
		}

		content.addQuestions(survey.getQuestions());

		activeSurveys.put(data.getToken(), content);
	}

	public Status getState(SurveyData data) {
		String token = data.getToken();
		if (!activeSurveys.containsKey(token))
			throw new IllegalArgumentException("Cannot load status: no model for token=" + token);

		return activeSurveys.get(token).getState();
	}

	private boolean isSkillValid() {
		// TODO
		throw new NotImplementedException();
	}

	public boolean isFinished(SurveyData data) {
		// TODO
		throw new NotImplementedException();
	}

	public Question getNextQuestion(SurveyData data) {
		// TODO
		throw new NotImplementedException();
	}

	public void complete(SurveyData data) {
		// TODO:
		//  - remove inference and model from map
		//  - collect results
		throw new NotImplementedException();
	}
}
