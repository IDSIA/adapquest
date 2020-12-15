package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.bp.BeliefPropagation;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIParser;
import lombok.Getter;

import java.util.*;
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

	protected Map<Skill, Integer> questionsDonePerSkill = new HashMap<>();
	protected Map<Skill, Set<QuestionLevel>> availableQuestions = new HashMap<>();

	@Getter
	protected QuestionLevel nextQuestionLevel = null;
	@Getter
	protected Skill nextSkill = null;

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
		for (Question q : questions) {
			Skill s = q.getSkill();
			questionsDonePerSkill.putIfAbsent(s, 0);
			availableQuestions.computeIfAbsent(s, x -> new HashSet<>()).add(q.getLevel());
		}
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
	public boolean isSkillValid(Skill skill) {
		Integer questionsDone = questionsDonePerSkill.get(skill);

		if (!availableQuestions.get(skill).isEmpty())
			return false;

		if (questionsDone <= model.getQuestionPerSkillMin())
			return true;

		return questionsDone <= model.getQuestionPerSkillMax();
	}

	/**
	 * Set a {@link Skill} to be invalid by reducing the number of {@link SkillLevel} to zero.
	 *
	 * @param skill the skill to invalidate
	 */
	public void invalidateSkill(Skill skill) {
		availableQuestions.put(skill, new HashSet<>());
	}

	/**
	 * Remove from the available questions, the given {@link QuestionLevel} for the given {@link Skill}.
	 *
	 * @param skill skill to remove a level from
	 * @param level level to remove
	 */
	public void removeSkillQuestionLevel(Skill skill, QuestionLevel level) {
		availableQuestions.get(skill).remove(level);
	}

	public abstract void check();

	public abstract void next();

}
