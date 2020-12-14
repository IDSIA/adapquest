package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.AdaptiveModel;
import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Skill;
import ch.idsia.adaptive.backend.persistence.model.Status;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.bp.BeliefPropagation;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIParser;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:18
 */
public abstract class AbstractSurvey {

	protected final AdaptiveModel model;
	protected final BayesianNetwork network;
	protected final BeliefPropagation<BayesianFactor> inference;

	protected Map<Skill, List<Question>> availableQuestions;
	protected Map<Skill, List<Question>> questionsDonePerSkill = new HashMap<>();

	protected Question nextQuestion = null;

	public AbstractSurvey(AdaptiveModel model) {
		this.model = model;

		List<String> lines = Arrays.stream(model.getData().split("\n")).collect(Collectors.toList());

		this.network = new BayesUAIParser(lines).parse();
		this.inference = new BeliefPropagation<>(network);
	}

	public Status getState() {
		Map<String, double[]> state = model.getSkillToVariable()
				.entrySet()
				.stream()
				.collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> inference.query(e.getValue()).getData()
				));

		return new Status().setState(state);
	}

	public void addQuestions(List<Question> questions) {
		availableQuestions = questions.stream().collect(
				Collectors.groupingBy(
						Question::getSkill,
						Collectors.toList()
				)
		);
	}

	public abstract void check();

	public abstract void next();

}
