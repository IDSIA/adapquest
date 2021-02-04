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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:18
 */
public abstract class AbstractSurvey {
	private static final Logger logger = LogManager.getLogger(AbstractSurvey.class);

	/**
	 * Reference survey.
	 */
	protected final Survey survey;
	/**
	 * Random generator.
	 */
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
	 * Questions who don't have an answer for each skill,
	 */
	protected final Map<Skill, LinkedList<Question>> availableQuestionsPerSkill = new HashMap<>();


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
		final Map<String, Skill> sks = new HashMap<>();
		final Map<String, double[]> state = new HashMap<>();
		final Map<String, Double> entropy = new HashMap<>();
		final Map<String, Long> qps = new HashMap<>();
		final Set<String> skillCompleted = new HashSet<>();

		for (Skill skill : skills) {
			String s = skill.getName();

			final BayesianFactor f = inference.query(skill.getVariable(), observations);
			final double h = BayesianEntropy.H(f);

			sks.put(skill.getName(), skill);
			state.put(s, f.getData());
			entropy.put(s, h);

			final long qdps = questionsDonePerSkill.get(skill).size();
			qps.put(s, qdps);

			if (qdps > survey.getQuestionPerSkillMax())
				skillCompleted.add(s);
		}

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
			this.availableQuestionsPerSkill.computeIfAbsent(skill, x -> new LinkedList<>()).add(q);
		});
	}

	public abstract boolean isFinished();

	public void register(Question q) {
		if (q == null) {
			currentQuestion = null;
			return;
		}

		Skill s = q.getSkill();

		// remove from possible questions
		availableQuestionsPerSkill.get(s).remove(q);
		questions.remove(q);

		// add to done slacks
		questionsDonePerSkill.get(s).add(q);
		questionsDone.add(q);

		// update current question
		currentQuestion = q;

		logger.debug("next question is skill={} question={}", s.getName(), q.getName());
	}

	public void check(Answer answer) {
		final Integer variable = answer.getQuestion().getVariable();
		final Integer state = answer.getQuestionAnswer().getState();
		observations.put(variable, state);
		answered = true;
	}

	public abstract Question next() throws SurveyException;
}
