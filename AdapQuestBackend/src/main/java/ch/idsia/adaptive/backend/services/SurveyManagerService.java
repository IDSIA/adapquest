package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.services.commons.SurveyException;
import ch.idsia.adaptive.backend.services.commons.agents.*;
import ch.idsia.adaptive.backend.services.commons.scoring.Scoring;
import ch.idsia.adaptive.backend.services.commons.scoring.precise.ScoringFunctionBayesianMode;
import ch.idsia.adaptive.backend.services.commons.scoring.precise.ScoringFunctionExpectedEntropy;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    03.12.2020 09:48
 */
@Service
public class SurveyManagerService {
	private static final Logger logger = LoggerFactory.getLogger(SurveyManagerService.class);

	private final SurveyRepository surveyRepository;

	private final Map<String, Agent> activeSurveys = new ConcurrentHashMap<>();

	// TODO: better parallel management with a scheduler
	final int PARALLEL_COUNT = Runtime.getRuntime().availableProcessors() / 2;
	final ExecutorService es = Executors.newFixedThreadPool(PARALLEL_COUNT);

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
		final Long surveyId = data.getSurveyId();
		final Survey survey = surveyRepository
				.findById(surveyId)
				.orElseThrow(() -> new IllegalArgumentException("No model associated with SurveyId=" + surveyId));

		final Long seed = data.getStartTime().toEpochSecond(OffsetDateTime.now().getOffset());

		final AgentGeneric<BayesianFactor> agent = getAgentForSurvey(survey, seed);
		agent.setExecutor(es);

		activeSurveys.put(data.getToken(), agent);
	}

	public static AgentGeneric<BayesianFactor> getAgentForSurvey(Survey survey, Long seed) {
		final AgentGeneric<BayesianFactor> agent;

		// TODO: allow also imprecise agents

		if (survey.getIsAdaptive()) {
			Scoring<BayesianFactor> scoring;

			if (survey.getScoring().equals("mode")) {
				logger.debug("using ScoringFunctionBayesianMode for {}", survey.getId());
				scoring = new ScoringFunctionBayesianMode();
			} else {
				logger.debug("using ScoringFunctionExpectedEntropy for {}", survey.getId());
				scoring = new ScoringFunctionExpectedEntropy();
			}

			if (survey.getIsStructural()) {
				logger.debug("new AgentPreciseAdaptiveStructural for {}", survey.getId());
				agent = new AgentPreciseAdaptiveStructural(survey, seed, scoring);
			} else if (survey.getIsSimple()) {
				logger.debug("new AgentPreciseAdaptiveSimple for {}", survey.getId());
				agent = new AgentPreciseAdaptiveSimple(survey, seed, scoring);
			} else {
				logger.debug("new AgentPreciseAdaptiveSimple for {}", survey.getId());
				agent = new AgentPreciseAdaptive(survey, seed, scoring);
			}

		} else {
			logger.debug("new AgentPreciseNonAdaptive for {}", survey.getId());
			agent = new AgentPreciseNonAdaptive(survey, seed);
		}

		return agent;
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

	public boolean checkAnswer(SurveyData data, Answer answer) {
		return getSurvey(data).check(answer);
	}

	public Question nextQuestion(SurveyData data) throws SurveyException {
		return getSurvey(data).next();
	}

	public List<Question> rankQuestions(SurveyData data) throws SurveyException {
		return getSurvey(data).rank();
	}

	public void complete(SurveyData data) {
		activeSurveys.remove(data.getToken());
		// TODO: collect results
	}
}
