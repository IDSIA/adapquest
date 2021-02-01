package ch.idsia.adaptive.experiments.models;

import lombok.AllArgsConstructor;

/**
 * Dummy class to identify a network node for a question.
 */
@AllArgsConstructor
class Question {
	int q, skill, difficulty;
	String idx;

	@Override
	public String toString() {
		return "Question{" +
				"q=" + q +
				", skill=" + skill +
				", difficulty=" + difficulty +
				'}';
	}
}
