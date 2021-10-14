package ch.idsia.adaptive.backend.persistence.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    19.01.2021 12:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AnswerStructure {

	/**
	 * Text of the question.
	 */
	public String text = "";

	/**
	 * If the survey is intended as a assessment test, this can be used to mark one answer as "correct".
	 */
	public Boolean correct = false;

	/**
	 * If the parent {@link QuestionStructure} is NOT a multiple-choice question, this state index refers to the index
	 * in the model associated with this answer on the parent question node.
	 */
	public Integer state = 1;

	/**
	 * If the parent {@link QuestionStructure} is a multiple-choice, this will be the reference of the variable in the model.
	 * Note that for multiple-choice answers, state 1 is checked true while state 0 is checked false.
	 */
	public Integer variable = -1;

	public AnswerStructure(String text, Integer variable, Integer state) {
		this.text = text;
		this.variable = variable;
		this.state = state;
	}
}
