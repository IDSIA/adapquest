package ch.idsia.adaptive.experiments.kitt4sme;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    29.09.2021 17:48
 */
public class KBinaryQuestion {

	final int questionId;
	final int answerId;
	final int binaryQuestionId;
	final boolean mandatory;
	final boolean yesOnly;
	final String questionText;
	final String binaryQuestionText;

	final Map<String, Double> values = new HashMap<>();

	public KBinaryQuestion(int questionId, int answerId, int binaryQuestionId, boolean mandatory, boolean yesOnly, String questionText, String binaryQuestionText) {
		this.questionId = questionId;
		this.answerId = answerId;
		this.binaryQuestionId = binaryQuestionId;
		this.mandatory = mandatory;
		this.yesOnly = yesOnly;
		this.questionText = questionText;
		this.binaryQuestionText = binaryQuestionText;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KBinaryQuestion that = (KBinaryQuestion) o;
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
				", answerId=" + answerId +
				", mandatory=" + mandatory +
				", questionText=" + questionText +
				", binaryQuestionText=" + binaryQuestionText +
				", values=" + values +
				'}';
	}
}
