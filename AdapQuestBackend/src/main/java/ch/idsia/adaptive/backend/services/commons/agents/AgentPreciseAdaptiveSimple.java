package ch.idsia.adaptive.backend.services.commons.agents;

import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Skill;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.commons.SurveyException;
import ch.idsia.adaptive.backend.services.commons.scoring.Scoring;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    14.12.2020 17:17
 */
public class AgentPreciseAdaptiveSimple extends AgentPrecise {
	private static final Logger logger = LogManager.getLogger(AgentPreciseAdaptiveSimple.class);

	public AgentPreciseAdaptiveSimple(Survey model, Long seed, Scoring<BayesianFactor> scoring) {
		super(model, seed, scoring);
	}

	@Override
	public boolean checkStop() {
		if (questions.isEmpty()) {
			// we don't have any more question
			logger.debug("survey finished with no more available questions");
			return true;
		}

		if (questionsDone.size() > survey.getQuestionTotalMax()) {
			// we made too many questions
			logger.debug("survey finished with too many questions (done={}, max={})", questionsDone.size(), survey.getQuestionTotalMax());
			return true;
		}

		if (questionsDone.size() < survey.getQuestionTotalMin()) {
			// we need to make more questions and there are skills that are still valid
			return false;
		}

		// check entropy levels
		double h = 0;
		for (Skill skill : skills) {
			Integer S = skill.getVariable();

			final BayesianFactor pS = inference.query(model, observations, S);
			final double HS = scoring.score(pS);

			h += HS;
		}

		h /= skills.size();

		if (h < survey.getGlobalMeanEntropyLowerThreshold() || h > survey.getGlobalMeanEntropyUpperThreshold()) {
			logger.debug("survey finished because the mean global entropy threshold was reached (H={}, lower={}, upper={})",
					h, survey.getGlobalMeanEntropyLowerThreshold(), survey.getGlobalMeanEntropyUpperThreshold());
			return true;
		}

		// all skills are depleted?
		final boolean b = questionsAvailablePerSkill.values().stream().allMatch(Collection::isEmpty);

		if (b)
			logger.debug("survey finished with no more valid skills");

		return b;
	}

	@Override
	public Question nextQuestion() throws SurveyException {
		// find the question with the optimal entropy
		Question nextQuestion = null;
		double maxIG = -Double.MAX_VALUE;

		Map<Skill, Double> HSs = new HashMap<>();
		for (Skill skill : skills) {
			final Integer S = skill.getVariable();
			final BayesianFactor PS = inference.query(model, observations, S);
			final double HS = scoring.score(PS); // skill score
			HSs.put(skill, HS);
		}

		for (Question question : questions) {
			final Integer Q = question.getVariable();
			final int size = model.getSize(Q);

			final BayesianFactor PQ = inference.query(model, observations, Q);

			double meanInfoGain = 0;
			for (Skill skill : skills) {
				final Integer S = skill.getVariable();
				final Double HS = HSs.get(skill);

				double HSQ = 0;

				for (int i = 0; i < size; i++) {
					final TIntIntMap qi = new TIntIntHashMap(observations);
					qi.put(Q, i);

					final BayesianFactor PSqi = inference.query(model, qi, S);
					final double Pqi = PQ.getValue(i);
					double HSqi = scoring.score(PSqi);
					HSqi = Double.isNaN(HSqi) ? 0.0 : HSqi;

					HSQ += HSqi * Pqi; // conditional entropy
				}

//				logger.debug("question={} skill={} with HSQ={}", question.getName(), skill.getName(), HSQ);

				meanInfoGain += Math.max(0, HS - HSQ) / skills.size();
			}

			logger.debug("question={} with average infoGain={}", question.getName(), meanInfoGain);

			if (meanInfoGain > maxIG) {
				maxIG = meanInfoGain;
				nextQuestion = question;
				nextQuestion.setScore(maxIG);
			}
		}

		if (questionsDone.size() >= survey.getQuestionTotalMin()) {
			double eps = 1e-9;
			if (maxIG <= eps) {
				logger.info("InfoGain below threshold: maxIG={} eps={}", maxIG, eps);
				finished = true;
				throw new SurveyException("Finished");
			}
		}

		return nextQuestion;
	}
}
