package ch.idsia.adaptive.backend.services.commons.agents;

import ch.idsia.adaptive.backend.persistence.model.Skill;
import ch.idsia.adaptive.backend.persistence.model.State;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.commons.inference.precise.InferenceLBP;
import ch.idsia.adaptive.backend.services.commons.scoring.Scoring;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.model.io.uai.BayesUAIParser;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    25.05.2021 10:19
 */
public abstract class AgentPrecise extends AgentGeneric<BayesianFactor> {

	public AgentPrecise(Survey survey, Long seed, Scoring<BayesianFactor> scoringFunction) {
		super(survey, seed, scoringFunction);

		final List<String> lines = Arrays.stream(survey.getModelData().split("\n")).collect(Collectors.toList());

		this.model = new BayesUAIParser(lines).parse();
		this.inference = new InferenceLBP();
	}

	@Override
	public State getState() {
		final State state = new State();

		double avgScore = 0.0;
		for (Skill skill : skills) {
			final String s = skill.getName();

			final Integer v = skill.getVariable();
			final BayesianFactor f;

			if (observations.containsKey(v))
				f = BayesianFactorFactory.factory().domain(model.getDomain(v)).set(1.0, observations.get(v)).get();
			else
				f = inference.query(model, observations, v);

			final double h = scoring.score(f);
			avgScore += h / skills.size();

			state.getSkills().put(skill.getName(), skill);
			state.getProbabilities().put(s, f.getData());
			state.getScore().put(s, h);

			if (questionsDonePerSkill.containsKey(skill)) {
				final long qdps = questionsDonePerSkill.get(skill).size();
				state.getQuestionsPerSkill().put(s, qdps);

				if (qdps > survey.getQuestionPerSkillMax())
					state.getSkillCompleted().add(s);
			}
		}

		state.setScoreAverage(avgScore);
		state.setTotalAnswers(questionsDone.size());

		return state;
	}

}
