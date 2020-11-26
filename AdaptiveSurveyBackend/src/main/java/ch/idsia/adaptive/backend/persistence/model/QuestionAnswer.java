package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    26.11.2020 11:59
 */
@Entity
@Data
@NoArgsConstructor
public class QuestionAnswer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Text for this answer.
	 */
	private String text;

	/**
	 * True if this is a correct answer.
	 */
	private Boolean isCorrect;

	public QuestionAnswer(String text) {
		this.text = text;
	}

	public QuestionAnswer(String text, Boolean isCorrect) {
		this.text = text;
		this.isCorrect = isCorrect;
	}
}
