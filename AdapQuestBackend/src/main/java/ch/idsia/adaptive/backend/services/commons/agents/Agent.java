package ch.idsia.adaptive.backend.services.commons.agents;

import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.State;
import ch.idsia.adaptive.backend.services.commons.SurveyException;
import ch.idsia.adaptive.backend.services.commons.scoring.Scoring;

import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    21.05.2021 09:24
 */
public interface Agent {

	/**
	 * @return return the current {@link State} of the survey.
	 */
	State getState();

	/**
	 * Checks the answer to the given question.
	 *
	 * @param answer answer given
	 * @return true if the answer was correctly recorded, otherwise (as an example, in case the answer is for the wrong
	 * question) false.
	 */
	boolean check(Answer answer);

	/**
	 * @return true if stop conditions are met, otherwise false.
	 */
	boolean stop();

	/**
	 * @return the next {@link Question} found using the given {@link Scoring}.
	 */
	Question next() throws SurveyException;

	/**
	 * @return an ordered list of {@link Question} ordered by the score of each {@link Question}. The first question of
	 * this list should be the same as the question obtained with the {@link #next()} method.
	 */
	List<Question> rank() throws SurveyException;

}
