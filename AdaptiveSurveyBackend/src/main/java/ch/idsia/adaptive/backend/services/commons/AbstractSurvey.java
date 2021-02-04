package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.crema.entropy.BayesianEntropy;
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

	/**
	 * Available skill in no specific order.
	 */
	protected final Set<Skill> skills = new HashSet<>();

	/**
	 * List of all the questions available in the survey.
	 */
	protected final LinkedList<Question> questions = new LinkedList<>();
	/**
	 * Questions who already have an answer.
	 */
	protected final LinkedList<Question> questionsDone = new LinkedList<>();
	/**
	 * Questions who already have an answer for each skill.
	 */
	protected final Map<Skill, List<Question>> questionsDonePerSkill = new HashMap<>();

	/**
	 * Model associated with this survey.
	 */
	protected final BayesianNetwork network;
	/**
	 * Inference engine.
	 */
	protected final BeliefPropagation<BayesianFactor> inference;

	protected final TIntIntMap observations = new TIntIntHashMap();

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
		Map<String, double[]> state = new HashMap<>();
		Map<String, Double> entropy = new HashMap<>();

		Map<String, Skill> sks = new HashMap<>();

		for (Skill skill : skills) {
			String s = skill.getName();

			BayesianFactor f = inference.query(skill.getVariable(), observations);
			double[] distr = f.getData();
			double h = BayesianEntropy.H(f);

			sks.put(skill.getName(), skill);
			state.put(s, distr);
			entropy.put(s, h);
		}

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
				.setSkills(sks)
				.setState(state)
				.setEntropy(entropy)
				.setQuestionsPerSkill(qps)
				.setSkillCompleted(skillCompleted)
				.setTotalAnswers(questionsDone.size());
	}

	public void addQuestions(List<Question> questions) {
		questions.forEach(q -> {
			Skill skill = q.getSkill();

			this.questions.add(q);
			this.skills.add(skill);
			this.questionsDonePerSkill.putIfAbsent(skill, new LinkedList<>());
		});
	}

	public abstract boolean isFinished();

	public void check(Answer answer) {
		final Integer variable = answer.getQuestion().getVariable();
		final Integer state = answer.getQuestionAnswer().getState();
		observations.put(variable, state);
		answered = true;

		final Skill skill = answer.getQuestion().getSkill();
		questionsDonePerSkill.get(skill).add(answer.getQuestion());
		questionsDone.add(answer.getQuestion());
	}

	public abstract Question next() throws SurveyException;
}
