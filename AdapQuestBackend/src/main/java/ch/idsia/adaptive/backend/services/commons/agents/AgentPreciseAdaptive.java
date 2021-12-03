package ch.idsia.adaptive.backend.services.commons.agents;

import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Skill;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.commons.SurveyException;
import ch.idsia.adaptive.backend.services.commons.scoring.Scoring;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    14.12.2020 17:17
 * <p>
 * This specific {@link Agent} works on a model that have a single parent skill for each available question.
 * For a multi-skill model, consider using the {@link AgentPreciseAdaptiveSimple} agent.
 */
public class AgentPreciseAdaptive extends AgentPrecise {
	private static final Logger logger = LoggerFactory.getLogger(AgentPreciseAdaptive.class);

	public AgentPreciseAdaptive(Survey survey, Long seed, Scoring<BayesianFactor> scoring) {
		super(survey, seed, scoring);
		addSkills(survey.getSkills());
		addQuestions(survey.getQuestions());
	}

	public boolean isSkillValid(Skill skill) {
		final BayesianFactor PS = inference.query(model, observations, skill.getVariable());
		final double HS = scoring.score(PS);
		return isSkillValid(skill, HS);
	}

	/**
	 * Check if the given {@link Skill} is valid in the current state or not. The condition for a {@link Skill} to be
	 * valid are:
	 * <li>the number of questions are below the minimum;</li>
	 * <li>the number of questions are below are above the maximum;</li>
	 * <li>there still are questions available.</li>
	 *
	 * @param skill the skill to test
	 * @return true if the skill is valid, otherwise false.
	 */
	public boolean isSkillValid(Skill skill, double score) {
		final Long questionsDone = (long) questionsDonePerSkill.get(skill).size();

		if (questionsAvailablePerSkill.get(skill).isEmpty()) {
			// the skill has no questions available
			logger.debug("skill={} has no questions available", skill.getName());
			return false;
		}

		if (questionsDone <= survey.getQuestionPerSkillMin()) {
			// we need to make more questions for this skill
			return true;
		}

		if (questionsDone > survey.getQuestionPerSkillMax()) {
			logger.debug("skill={} reached max questions per skill (done= {}, max={})", skill.getName(), questionsDone, survey.getQuestionPerSkillMax());
			return false;
		}

		if (this.questionsDone.size() < survey.getQuestionTotalMin()) {
			return true;
		}

		if (score > survey.getScoreUpperThreshold()) {
			// skill score level achieved
			logger.debug("skill={} has too low score={} (upper={})", skill.getName(), score, survey.getScoreUpperThreshold());
			return false;
		}

		if (score < survey.getScoreLowerThreshold()) {
			// skill score level achieved
			logger.debug("skill={} has too low score={} (lower={})", skill.getName(), score, survey.getScoreLowerThreshold());
			return false;
		}

		return true;
	}

	@Override
	public boolean checkStop() {
		if (questions.isEmpty()) {
			// we don't have any more question
			logger.debug("survey finished with no more available questions");
			return true;
		}

		if (questionsDone.size() >= survey.getQuestionTotalMax()) {
			// we made too many questions
			logger.debug("survey finished with too many questions (done={}, max={})", questionsDone.size(), survey.getQuestionTotalMax());
			return true;
		}

		if (questionsDone.size() < survey.getQuestionTotalMin() && skills.stream().anyMatch(this::isSkillValid)) {
			// we need to make more questions and there are skills that are still valid
			return false;
		}

		// check score levels
		double h = 0;
		for (Skill skill : skills) {
			Integer S = skill.getVariable();

			final BayesianFactor pS = inference.query(model, observations, S);
			final double HS = scoring.score(pS);

			h += HS;
		}

		h /= skills.size();

		if (h < survey.getGlobalMeanScoreLowerThreshold() || h > survey.getGlobalMeanScoreUpperThreshold()) {
			logger.debug("survey finished because the mean global score threshold reached (H={}, lower={}, upper={})",
					h, survey.getGlobalMeanScoreLowerThreshold(), survey.getGlobalMeanScoreUpperThreshold());
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

		for (Skill skill : skills) {
			final Integer S = skill.getVariable();
			final BayesianFactor PS = inference.query(model, observations, S);
			final double HS = scoring.score(PS); // skill score

			if (!isSkillValid(skill, HS)) {
				logger.debug("skill={} is not valid", skill.getName());
				continue;
			}

			for (Question question : questionsAvailablePerSkill.get(skill)) {
				final Integer Q = question.getVariable();
				final BayesianFactor PQ = inference.query(model, observations, Q);
				final int size = model.getSize(Q);

				double HSQ = 0;

				for (int i = 0; i < size; i++) {
					final TIntIntMap qi = new TIntIntHashMap(observations);
					question.getQuestionAnswer(Q, i).observe(qi);

					final BayesianFactor PSqi = inference.query(model, qi, S);
					final double Pqi = PQ.getValue(i);
					double HSqi = scoring.score(PSqi);
					HSqi = Double.isNaN(HSqi) ? 0.0 : HSqi;

					HSQ += HSqi * Pqi; // conditional score
				}

				final double infoGain = Math.max(0, HS - HSQ);
				question.setScore(infoGain);

				logger.debug("skill={} question={} with average infoGain={}", skill.getName(), question.getName(), infoGain);

				if (infoGain > maxIG) {
					maxIG = infoGain;
					nextQuestion = question;
				}
			}
		}

		if (nextQuestion == null)
			// this is also valid for nextSkill == null
			throw new SurveyException("No valid question found!");

		return nextQuestion;
	}

}
