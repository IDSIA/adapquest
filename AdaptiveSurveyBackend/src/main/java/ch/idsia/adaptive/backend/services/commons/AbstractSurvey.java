package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.bp.BeliefPropagation;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIParser;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:18
 */
public abstract class AbstractSurvey {

	protected final Survey survey;
	protected final Random random;

	protected final BayesianNetwork network;
	protected final BeliefPropagation<BayesianFactor> inference;

	protected TIntIntMap answers = new TIntIntHashMap();

	@Getter
	protected QuestionLevel nextQuestionLevel = null;
	@Getter
	protected Skill nextSkill = null;

	public AbstractSurvey(Survey survey, Long seed) {
		this.survey = survey;
		this.random = new Random(seed);

		List<String> lines = Arrays.stream(survey.getModelData().split("\n")).collect(Collectors.toList());

		this.network = new BayesUAIParser(lines).parse();
		this.inference = new BeliefPropagation<>(network);
	}

	public Status getState() {
		Map<String, double[]> state = survey.getSkillToVariable()
				.entrySet()
				.stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> {
							inference.setEvidence(answers);
							return inference.query(e.getValue()).getData();
						}
				));

		return new Status().setState(state);
	}

	public abstract void addQuestions(List<Question> questions);

	public abstract boolean isFinished();

	public void check(Answer answer) {
		Integer s = survey.getVariable(answer.getQuestion());
		// TODO: get the correct value from answer.answerGiven
		answers.put(s, 1);
	}

	public abstract Question next();
}
