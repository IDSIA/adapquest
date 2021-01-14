package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.crema.entropy.BayesianEntropy;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:17
 */
public class AdaptiveSurvey extends NonAdaptiveSurvey {
	protected Map<String, Skill> nameToSkill = new HashMap<>();

	protected Map<Skill, Integer> questionsDonePerSkill = new HashMap<>();
	protected Map<Skill, Set<QuestionLevel>> availableQuestionLevels = new HashMap<>();
	protected Map<Pair<Skill, QuestionLevel>, LinkedList<Question>> availableQuestions = new HashMap<>();


	public AdaptiveSurvey(Survey model, Long seed) {
		super(model, seed);
	}

	@Override
	public void addQuestions(List<Question> questions) {
		questions.forEach(q -> {
			Skill skill = q.getSkill();

			// for AbstractSurvey class
			this.questions.add(q);
			this.skills.add(skill);
			this.levels.computeIfAbsent(skill, i -> new HashSet<>()).add(q.getLevel());

			// for this class
			this.nameToSkill.putIfAbsent(skill.getName(), skill);
			this.questionsDonePerSkill.putIfAbsent(skill, 0);
			this.availableQuestionLevels.computeIfAbsent(skill, x -> new TreeSet<>()).add(q.getLevel());
			this.availableQuestions.computeIfAbsent(new ImmutablePair<>(skill, q.getLevel()), x -> new LinkedList<>()).add(q);
		});
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

		if (!availableQuestionLevels.get(skill).isEmpty())
			return false;

		if (questionsDone <= survey.getQuestionPerSkillMin())
			return true;

		return questionsDone <= survey.getQuestionPerSkillMax();
	}

	/**
	 * Set a {@link Skill} to be invalid by reducing the number of {@link SkillLevel} to zero.
	 *
	 * @param skill the skill to invalidate
	 */
	public void invalidateSkill(Skill skill) {
		availableQuestionLevels.put(skill, new HashSet<>());
	}

	/**
	 * Remove from the available questions, the {@link QuestionLevel} and {@link Skill} associated with the given
	 * {@link Question}.
	 *
	 * @param question question to remove
	 */
	public void removeSkillQuestionLevel(Question question) {
		Skill skill = question.getSkill();
		QuestionLevel level = question.getLevel();
		ImmutablePair<Skill, QuestionLevel> key = new ImmutablePair<>(skill, level);
		availableQuestions.get(key).remove(question);
		availableQuestionLevels.get(skill).remove(level);
	}

	@Override
	public boolean isFinished() {
		return availableQuestionLevels.values().stream().allMatch(Set::isEmpty);
	}

	@Override
	public Question next() {
		if (!answered && currentQuestion != null)
			return currentQuestion;

		Skill nextSkill = null;
		QuestionLevel nextLevel = null;
		double minH = Double.MAX_VALUE;

		for (Skill skill : skills) {
			Integer S = skill.getVariable();

			// TODO: refactor since we will have questions in the model instead of question levels

			for (QuestionLevel level : levels.get(skill)) {
				Integer L = level.getVariable();

				int size = network.getSize(L);

				double h = 0;

				for (int i = 0; i < size; i++) {
					inference.clearEvidence();
					TIntIntMap obs = new TIntIntHashMap();
					obs.put(L, i);
					inference.setEvidence(obs);

					BayesianFactor pS = inference.query(S);
					h += BayesianEntropy.H(pS);
				}

				if (h < minH) {
					nextSkill = skill;
					nextLevel = level;
					minH = h;
				}
			}
		}

		LinkedList<Question> questions = availableQuestions.get(new ImmutablePair<>(nextSkill, nextLevel));

		// TODO:
		//  - add checks for nextSkill and nextLevel!
		//  - Consider conditional entropy of skill
		//  - register which question has been chose
		//  - check if enough questions
		//  - random choice of question
		//  - call other uncalled methods...

		return questions.get(0);
	}
}
