package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.services.commons.SurveyException;
import ch.idsia.adaptive.backend.services.commons.agents.*;
import ch.idsia.adaptive.backend.services.commons.scoring.Scoring;
import ch.idsia.adaptive.backend.services.commons.scoring.precise.ScoringFunctionBayesianMode;
import ch.idsia.adaptive.backend.services.commons.scoring.precise.ScoringFunctionExpectedEntropy;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    03.12.2020 09:48
 */
@Service
public class SurveyManagerService {

	private final SurveyRepository surveyRepository;

	private final Map<String, Agent> activeSurveys = new ConcurrentHashMap<>();

	@Autowired
	public SurveyManagerService(SurveyRepository surveyRepository) {
		this.surveyRepository = surveyRepository;
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

		Long seed = data.getStartTime().toEpochSecond(OffsetDateTime.now().getOffset());

		// TODO: allow also imprecise agents
		AgentPrecise agent;

		if (survey.getIsAdaptive()) {
			Scoring<BayesianFactor> scoring;

			if (survey.getScoring().equals("mode"))
				scoring = new ScoringFunctionBayesianMode();
			else
				scoring = new ScoringFunctionExpectedEntropy();

			if (survey.getIsSimple()) {
				agent = new AgentPreciseAdaptiveSimple(survey, seed, scoring);
			} else {
				agent = new AgentPreciseAdaptive(survey, seed, scoring);
			}
		} else {
			agent = new AgentPreciseNonAdaptive(survey, seed);
		}
		agent.addSkills(survey.getSkills());
		agent.addQuestions(survey.getQuestions());

		activeSurveys.put(data.getToken(), agent);
	}

	public Agent getSurvey(SurveyData data) {
		String token = data.getToken();
		return Optional.ofNullable(activeSurveys.get(token))
				.orElseThrow(() -> new IllegalArgumentException("Cannot load status: no model for token=" + token));
	}

	public State getState(SurveyData data) {
		return getSurvey(data).getState();
	}

	public boolean isFinished(SurveyData data) {
		return getSurvey(data).stop();
	}

	public void checkAnswer(SurveyData data, Answer answer) {
		getSurvey(data).check(answer);
	}

	public Question nextQuestion(SurveyData data) throws SurveyException {
		return getSurvey(data).next();
	}

	public void complete(SurveyData data) {
		activeSurveys.remove(data.getToken());
		// TODO: collect results
	}
}
