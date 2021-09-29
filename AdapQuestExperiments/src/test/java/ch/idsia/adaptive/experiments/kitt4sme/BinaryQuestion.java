package ch.idsia.adaptive.experiments.kitt4sme;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    29.09.2021 17:48
 */
public class BinaryQuestion {

	final int questionId;
	final boolean mandatory;
	final String questionText;
	final String binaryQuestionText;

	final Map<String, Double> values = new HashMap<>();

	public BinaryQuestion(int questionId, boolean mandatory, String questionText, String binaryQuestionText) {
		this.questionId = questionId;
		this.mandatory = mandatory;
		this.questionText = questionText;
		this.binaryQuestionText = binaryQuestionText;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		BinaryQuestion that = (BinaryQuestion) o;
		return Objects.equals(binaryQuestionText, that.binaryQuestionText);
	}

	@Override
	public int hashCode() {
		return Objects.hash(binaryQuestionText);
	}

	@Override
	public String toString() {
		return "BinaryQuestion{" +
				"questionId=" + questionId +
				", mandatory=" + mandatory +
				", questionText=" + questionText +
				", binaryQuestionText=" + binaryQuestionText +
				", values=" + values +
				'}';
	}
}
