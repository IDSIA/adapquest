package ch.idsia.adaptive.backend.services.templates;

import java.util.Objects;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    29.09.2021 17:11
 * <p>
 * This is just a support class for parsing binary questions during the reading of a template.
 */
public class TQuestion {
	final int questionId;
	final boolean mandatory;
	final boolean exclusivity;
	final String questionText;

	public TQuestion(int questionId, boolean mandatory, boolean exclusivity, String questionText) {
		this.questionId = questionId;
		this.mandatory = mandatory;
		this.exclusivity = exclusivity;
		this.questionText = questionText;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TQuestion question = (TQuestion) o;
		return questionText.equals(question.questionText);
	}

	@Override
	public int hashCode() {
		return Objects.hash(questionText);
	}

	@Override
	public String toString() {
		return "Question{" +
				"questionId=" + questionId +
				", mandatory=" + mandatory +
				", exclusivity=" + exclusivity +
				", questionText='" + questionText + '\'' +
				'}';
	}
}
