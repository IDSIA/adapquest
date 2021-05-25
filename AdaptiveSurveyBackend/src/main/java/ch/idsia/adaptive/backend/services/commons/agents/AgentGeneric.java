package ch.idsia.adaptive.backend.services.commons.agents;

import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Skill;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.commons.SurveyException;
import ch.idsia.adaptive.backend.services.commons.inference.InferenceEngine;
import ch.idsia.adaptive.backend.services.commons.scoring.Scoring;
import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.model.graphical.DAGModel;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import lombok.Getter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.Collectors;

import static ch.idsia.adaptive.backend.config.Consts.NO_SKILL;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:18
 */
public abstract class AgentGeneric<F extends GenericFactor> implements Agent {
	private static final Logger logger = LogManager.getLogger(AgentGeneric.class);

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
	 * List of mandatory questions.
	 */
	protected final LinkedList<Question> mandatoryQuestions = new LinkedList<>();
	/**
	 * Questions who already have an answer.
	 */
	protected final LinkedList<Question> questionsDone = new LinkedList<>();
	/**
	 * Questions who already have an answer for each skill.
	 */
	protected final Map<Skill, LinkedList<Question>> questionsDonePerSkill = new HashMap<>();
	/**
	 * Questions who don't have an answer for each skill,
	 */
	protected final Map<Skill, LinkedList<Question>> questionsAvailablePerSkill = new HashMap<>();


	/**
	 * Model associated with this survey.
	 */
	protected DAGModel<F> model;
	/**
	 * Inference engine.
	 */
	protected InferenceEngine<F> inference;
	/**
	 * Evidence map of past answers.
	 */
	protected final TIntIntMap observations = new TIntIntHashMap();

	@Getter
	protected boolean answered = false;
	@Getter
	protected Question currentQuestion = null;
	@Getter
	protected Boolean finished = false;

	protected final Scoring<F> scoring;

	public AgentGeneric(Survey survey, Long seed, Scoring<F> scoring) {
		this.survey = survey;
		this.random = new Random(seed);
		this.scoring = scoring;
	}

	public void addSkills(Set<Skill> skills) {
		this.skills.addAll(
				skills.stream()
						.filter(Objects::nonNull)
						.filter(x -> !x.getName().equals(NO_SKILL))
						.collect(Collectors.toList())
		);
	}

	public void addQuestions(Set<Question> questions) {
		questions.forEach(q -> {
			Skill skill = q.getSkill();
			this.questions.add(q);
			this.questionsDonePerSkill.putIfAbsent(skill, new LinkedList<>());
			this.questionsAvailablePerSkill.computeIfAbsent(skill, x -> new LinkedList<>()).add(q);
			if (q.getMandatory()) {
				this.mandatoryQuestions.add(q);
			}
		});
		this.questions.sort(Comparator.comparingInt(Question::getVariable));
	}

	private void register(Question q) {
		if (q == null) {
			currentQuestion = null;
			return;
		}

		Skill s = q.getSkill();

		// remove from possible questions
		questionsAvailablePerSkill.get(s).remove(q);
		questions.remove(q);
		if (q.getMandatory())
			mandatoryQuestions.remove(q);

		// add to done slacks
		questionsDonePerSkill.get(s).add(q);
		questionsDone.add(q);

		// update current question
		currentQuestion = q;

		logger.debug("next question is skill={} question={}", s.getName(), q.getName());
		answered = false;
	}

	@Override
	public boolean stop() {
		if (finished)
			return true;

		finished = checkStop();
		return finished;
	}

	@Override
	public boolean check(Answer answer) {
		if (currentQuestion != null && currentQuestion.getVariable().equals(answer.getQuestion().getVariable())) {
			final Integer variable = answer.getQuestion().getVariable();
			final Integer state = answer.getQuestionAnswer().getState();
			observations.put(variable, state);
			answered = true;
			return true;
		}
		return false;
	}

	@Override
	public Question next() throws SurveyException {
		if (!answered && currentQuestion != null)
			return currentQuestion;

		if (finished) {
			throw new SurveyException("Survey is finished");
		}

		Question question;
		if (mandatoryQuestions.isEmpty()) {
			// find the next question using the specific algorithms...
			question = nextQuestion();
		} else {
			// ... else first empty the mandatory questions
			question = mandatoryQuestions.getFirst();
		}

		// register the chosen question as the next question
		register(question);

		return question;
	}

	/**
	 * Override this method to implement your design for the stop criteria.
	 *
	 * @return true if the survery is completed, otherwise false
	 */
	protected abstract boolean checkStop();

	/**
	 * Override this method to implement your design for the adaptive search of the next {@link Question} to ask. See
	 * method {@link #next()} for the stuff that you <u>don't</u> need to check in you code, such as checking the current
	 * question or register the next question before returning.
	 *
	 * @return a valid {@link Question} not null
	 * @throws SurveyException you can throw this if something bad happens
	 */
	protected abstract Question nextQuestion() throws SurveyException;

}
