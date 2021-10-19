package ch.idsia.adaptive.backend.persistence.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

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
	 * Name of the variable controlled by this question.
	 */
	public String name = "";

	/**
	 * Text of the question.
	 */
	public String text = "";

	/**
	 * If the survey is intended as a assessment test, this can be used to mark one answer as "correct".
	 */
	public Boolean correct = false;

	/**
	 * If true, this answer cannot be selected.
	 */
	public Boolean hidden = false;

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

	/**
	 * If true, this will generate an evidence directly on the nodes specified by {@link #directEvidenceVariables} in the
	 * state specified by {@link #directEvidenceStates}.
	 */
	public Boolean directEvidence = false;

	/**
	 * If {@link #directEvidence} is true, then this will also set an evidence on the nodes identified by these
	 * variables with the states specified in {@link #directEvidenceStates}.
	 */
	public List<Integer> directEvidenceVariables = new ArrayList<>();

	/**
	 * If {@link #directEvidence} is true, then this will also set an evidence on the nodes identified by
	 * {@link #directEvidenceVariables} with these states.
	 */
	public List<Integer> directEvidenceStates = new ArrayList<>();

	public AnswerStructure(String text) {
		this.text = text;
	}

	public AnswerStructure(String text, Boolean correct) {
		this.text = text;
		this.correct = correct;
	}

	public AnswerStructure(String text, Integer variable, Integer state) {
		this.text = text;
		this.variable = variable;
		this.state = state;
	}
}
