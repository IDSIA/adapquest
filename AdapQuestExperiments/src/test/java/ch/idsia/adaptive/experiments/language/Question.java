package ch.idsia.adaptive.experiments.language;

import lombok.AllArgsConstructor;

/**
 * Dummy class to identify a network node for a question.
 */
@AllArgsConstructor
class Question {
	/**
	 * Variable index in the {@link ch.idsia.crema.model.graphical.BayesianNetwork} model.
	 */
	final int q;

	/**
	 * Variable index of the skill variable in the {@link ch.idsia.crema.model.graphical.BayesianNetwork} model.
	 */
	final int skill;

	/**
	 * Used only as a reference.
	 */
	final int difficulty;

	/**
	 * Name of this question.
	 */
	final String idx;

	@Override
	public String toString() {
		return "Question{" +
				"q=" + q +
				", skill=" + skill +
				", difficulty=" + difficulty +
				'}';
	}
}
