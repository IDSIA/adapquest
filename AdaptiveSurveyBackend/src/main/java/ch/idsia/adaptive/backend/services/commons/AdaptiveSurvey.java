package ch.idsia.adaptive.backend.services.commons;

import ch.idsia.adaptive.backend.persistence.model.*;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    14.12.2020 17:17
 */
public class AdaptiveSurvey extends AbstractSurvey {
	protected Map<String, Skill> nameToSkill = new HashMap<>();

	protected Map<Skill, Integer> questionsDonePerSkill = new HashMap<>();
	protected Map<Skill, Set<QuestionLevel>> availableQuestionLevels = new HashMap<>();
	protected Map<Pair<Skill, QuestionLevel>, LinkedList<Question>> availableQuestions = new HashMap<>();


	public AdaptiveSurvey(Survey model, Long seed) {
		super(model, seed);
	}

	@Override
	public void addQuestions(List<Question> questions) {
		for (Question q : questions) {
			Skill s = q.getSkill();
			nameToSkill.putIfAbsent(s.getName(), s);
			questionsDonePerSkill.putIfAbsent(s, 0);
			availableQuestionLevels.computeIfAbsent(s, x -> new TreeSet<>()).add(q.getLevel());
			availableQuestions.computeIfAbsent(new ImmutablePair<>(s, q.getLevel()), x -> new LinkedList<>()).add(q);
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
		// TODO: Consider conditional entropy of skill
		throw new NotImplementedException();
	}
}
