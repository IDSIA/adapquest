package ch.idsia.adaptive.experiments.kitt4sme;

import java.util.Objects;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    29.09.2021 17:11
 */
public class KQuestion {
	final int questionId;
	final int sectionId;
	final boolean mandatory;
	final boolean exclusivity;
	final String questionText;

	boolean yesOnly = false;

	public KQuestion(int questionId, int sectionId, boolean mandatory, boolean exclusivity, String questionText) {
		this.questionId = questionId;
		this.sectionId = sectionId;
		this.mandatory = mandatory;
		this.exclusivity = exclusivity;
		this.questionText = questionText;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KQuestion question = (KQuestion) o;
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
				", sectionId=" + sectionId +
				", mandatory=" + mandatory +
				", exclusivity=" + exclusivity +
				", questionText='" + questionText + '\'' +
				'}';
	}
}
