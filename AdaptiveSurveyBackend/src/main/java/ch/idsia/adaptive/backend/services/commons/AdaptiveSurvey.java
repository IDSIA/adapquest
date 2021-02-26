package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Skill;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.crema.entropy.BayesianEntropy;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:17
 */
public class AdaptiveSurvey extends AbstractSurvey {
	private static final Logger logger = LogManager.getLogger(AdaptiveSurvey.class);

	public AdaptiveSurvey(Survey model, Long seed) {
		super(model, seed);
	}

	public boolean isSkillValid(Skill skill) {
		final BayesianFactor PS = inference.query(skill.getVariable(), observations);
		final double HS = BayesianEntropy.H(PS); // skill entropy
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
	public boolean isSkillValid(Skill skill, double entropy) {
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

		if (entropy > survey.getEntropyUpperThreshold()) {
			// skill entropy level achieved
			logger.debug("skill={} has too low entropy={} (upper={})", skill.getName(), entropy, survey.getEntropyUpperThreshold());
			return false;
		}

		if (entropy < survey.getEntropyLowerThreshold()) {
			// skill entropy level achieved
			logger.debug("skill={} has too low entropy={} (lower={})", skill.getName(), entropy, survey.getEntropyLowerThreshold());
			return false;
		}

		return true;
	}

	@Override
	public boolean isFinished() {
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

		if (questionsDone.size() < survey.getQuestionTotalMin() && skills.stream().anyMatch(this::isSkillValid)) {
			// we need to make more questions and there are skills that are still valid
			return false;
		}

		// check entropy levels
		double h = 0;
		for (Skill skill : skills) {
			Integer S = skill.getVariable();

			final BayesianFactor pS = inference.query(S, observations);
			final double HS = BayesianEntropy.H(pS);

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
	public Question findNext() throws SurveyException {
		// find the question with the optimal entropy
		Question nextQuestion = null;
		double maxIG = -Double.MAX_VALUE;

		for (Skill skill : skills) {
			Integer S = skill.getVariable();

			final BayesianFactor PS = inference.query(S, observations);
			final double HS = BayesianEntropy.H(PS); // skill entropy

			if (!isSkillValid(skill, HS)) {
				logger.debug("skill={} is not valid", skill.getName());
				continue;
			}

			for (Question question : questionsAvailablePerSkill.get(skill)) {
				final Integer Q = question.getVariable();
				final int size = network.getSize(Q);

				double HSQ = 0;

				final BayesianFactor PSQ = inference.query(Q, observations);

				for (int i = 0; i < size; i++) {
					final TIntIntMap qi = new TIntIntHashMap(observations);
					qi.put(Q, i);

					final BayesianFactor PSq = inference.query(S, qi);
					double HSqi = BayesianEntropy.H(PSq);
					HSqi = Double.isNaN(HSqi) ? 0.0 : HSqi;

					final double PSqi = PSQ.getValue(i);

					HSQ += HSqi * PSqi; // conditional entropy
				}

				final double infoGain = Math.max(0, HS - HSQ);

				logger.debug("skill={} question={} with average infoGain={}", skill.getName(), question.getName(), infoGain);

				if (infoGain > maxIG) {
					nextQuestion = question;
					maxIG = infoGain;
				}
			}
		}

		if (nextQuestion == null)
			// this is also valid for nextSkill == null
			throw new SurveyException("No valid question found!");

		return nextQuestion;
	}
}
