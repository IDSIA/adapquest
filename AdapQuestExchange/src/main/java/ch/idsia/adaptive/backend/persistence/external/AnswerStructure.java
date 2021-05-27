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
	 * State index in the model associated with this answer.
	 */
	public Integer state = 1;

	/**
	 * If the survey is intended as a assessment test, this can be used to mark one answer as "correct".
	 */
	public Boolean correct = false;

	public AnswerStructure(String text, Integer state) {
		this.text = text;
		this.state = state;
	}
}
