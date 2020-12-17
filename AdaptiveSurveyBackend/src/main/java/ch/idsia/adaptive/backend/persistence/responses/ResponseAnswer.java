package ch.idsia.adaptive.backend.persistence.responses;

import ch.idsia.adaptive.backend.persistence.model.QuestionAnswer;
import lombok.NoArgsConstructor;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    17.12.2020 16:26
 */
@NoArgsConstructor
public class ResponseAnswer {
	public Long id;
	public String text;
	public Integer state;

	public ResponseAnswer(QuestionAnswer answer) {
		id = answer.getId();
		text = answer.getText();
		state = answer.getState();
	}
}
