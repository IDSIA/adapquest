package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

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
	 * Name of the skill.
	 */
	public String skill = "";

	/**
	 * Question for the final subject.
	 */
	public String question = "";

	/**
	 * Explanation of the question, if needed.
	 */
	public String explanation = "";

	/**
	 * Name of the variable node.
	 */
	public String name = "";

	/**
	 * Index of the variable node in the model.
	 */
	public Integer variable = -1;

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
	 * List of available answers.
	 */
	public List<AnswerStructure> answers = new ArrayList<>();

}
