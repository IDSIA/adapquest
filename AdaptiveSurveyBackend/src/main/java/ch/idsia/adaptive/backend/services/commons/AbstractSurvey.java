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

	protected Set<Skill> skills = new HashSet<>();

	protected LinkedList<Question> questions = new LinkedList<>();
	protected List<Question> questionsDone = new ArrayList<>();

	protected final BayesianNetwork network;
	protected final BeliefPropagation<BayesianFactor> inference;

	protected TIntIntMap observations = new TIntIntHashMap();

	@Getter
	protected boolean answered = false;
	@Getter
	protected Question currentQuestion = null;

	public AbstractSurvey(Survey survey, Long seed) {
		this.survey = survey;
		this.random = new Random(seed);

		List<String> lines = Arrays.stream(survey.getModelData().split("\n")).collect(Collectors.toList());

		this.network = new BayesUAIParser(lines).parse();
		this.inference = new BeliefPropagation<>(network);
	}

	public State getState() {
		inference.clearEvidence();

		Map<String, double[]> state = skills.stream()
				.collect(Collectors.toMap(
						Skill::getName,
						s -> {
							inference.setEvidence(observations);
							return inference.query(s.getVariable()).getData();
						}
				));

		Map<String, Long> qps = questionsDone.stream()
				.map(Question::getSkill)
				.collect(Collectors.groupingBy(
						Skill::getName,
						Collectors.counting()
				));

		Set<String> skillCompleted = qps.entrySet().stream()
				.filter(x -> x.getValue() > survey.getQuestionPerSkillMax())
				.map(Map.Entry::getKey)
				.collect(Collectors.toSet());

		return new State()
				.setState(state)
				.setQuestionsPerSkill(qps)
				.setSkillCompleted(skillCompleted)
				.setTotalAnswers(questionsDone.size());
	}

	public void addQuestions(List<Question> questions) {
		questions.forEach(q -> {
			Skill skill = q.getSkill();

			this.questions.add(q);
			this.skills.add(skill);
		});
	}

	public abstract boolean isFinished();

	public void check(Answer answer) {
		Integer variable = answer.getQuestion().getVariable();
		Integer state = answer.getQuestionAnswer().getState();
		observations.put(variable, state);
		answered = true;
	}

	public abstract Question next() throws SurveyException;
}
