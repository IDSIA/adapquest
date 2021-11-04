package ch.idsia.adaptive.experiments.kitt4sme;

import java.util.Objects;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    29.09.2021 17:15
 */
public class KAnswer {

	final int questionId;
	final int answerId;
	final String text;

	public KAnswer(int questionId, int answerId, String text) {
		this.questionId = questionId;
		this.answerId = answerId;
		this.text = text;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KAnswer answer = (KAnswer) o;
		return answerId == answer.answerId && text.equals(answer.text);
	}

	@Override
	public int hashCode() {
		return Objects.hash(answerId, text);
	}

	@Override
	public String toString() {
		return "Answer{" +
				"questionId=" + questionId +
				", answerId=" + answerId +
				", text='" + text + '\'' +
				'}';
	}
}
