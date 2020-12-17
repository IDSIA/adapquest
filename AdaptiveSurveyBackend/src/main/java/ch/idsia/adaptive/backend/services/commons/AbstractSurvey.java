package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.bp.BeliefPropagation;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIParser;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import lombok.Getter;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:18
 */
public abstract class AbstractSurvey {

	protected final Survey survey;
	protected final Random random;

	protected LinkedList<Question> questions = new LinkedList<>();

	protected final BayesianNetwork network;
	protected final BeliefPropagation<BayesianFactor> inference;

	protected TIntIntMap observations = new TIntIntHashMap();

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
		Map<String, double[]> state = survey.getSkills()
				.stream()
				.collect(Collectors.toMap(
						Skill::getName,
						s -> {
							inference.setEvidence(observations);
							return inference.query(s.getVariable()).getData();
						}
				));

		return new Status().setState(state);
	}

	public void addQuestions(List<Question> questions) {
		this.questions.addAll(questions);
	}

	public abstract boolean isFinished();

	public void check(Answer answer) {
		Integer variable = answer.getQuestion().getLevel().getVariable();
		Integer state = answer.getAnswerGiven().getState();
		observations.put(variable, state);
	}

	public abstract Question next();
}
