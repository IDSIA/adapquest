package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    19.01.2021 12:03
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class QuestionStructure {

	/**
	 * Explanation of the question, if needed.
	 */
	public String explanation = "";

	/**
	 * Question for the final subject.
	 */
	public String question = "";

	/**
	 * Name of the variable node.
	 */
	public String name = "";

	/**
	 * If the question need a weight (also intended as points) for final results.
	 */
	public Double weight = 1.;

	/**
	 * Mark this question as an example.
	 */
	public Boolean example = false;

	/**
	 * If the answers need to be randomly organized.
	 */
	public Boolean randomAnswers = false;

	/**
	 * If true, this question will be forced to be asked before the adaptive engine starts.
	 */
	public Boolean mandatory = false;

	/**
	 * If true, this will be considered a multiple-choice question. The relative {@link AnswerStructure} should have
	 * their own model variable associated.
	 */
	public Boolean multipleChoice = false;

	/**
	 * If true, and {@link #multipleChoice} is also true, only the evidence of checked (true) answers are considered by
	 * the inference engine.
	 */
	public Boolean yesOnly = false;

	/**
	 * If true, this question influence a number of {@link SkillStructure} greater than 1.
	 */
	public Boolean multipleSkills = false;

	/**
	 * List of available answers.
	 */
	public List<AnswerStructure> answers = new ArrayList<>();

	/**
	 * Names of the skills.
	 */
	public Set<String> skills = new HashSet<>();

	public QuestionStructure setSkill(String skill) {
		skills.add(skill);
		return this;
	}

}
