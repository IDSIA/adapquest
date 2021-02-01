package ch.idsia.adaptive.backend.persistence.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    19.01.2021 12:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class AnswerStructure {

	public String text = "";
	public Integer state = 1;
	public Boolean correct = false;

	public AnswerStructure(String text, Integer state) {
		this.text = text;
		this.state = state;
	}
}
