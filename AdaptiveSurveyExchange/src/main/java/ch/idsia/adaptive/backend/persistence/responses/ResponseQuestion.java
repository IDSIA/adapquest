package ch.idsia.adaptive.backend.persistence.responses;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    16.12.2020 18:00
 */
public class ResponseQuestion {

	/**
	 * Id of the question (include this in your {@link ch.idsia.adaptive.backend.persistence.requests.RequestAnswer}).
	 */
	public Long id;

	/**
	 * Name of the model variable.
	 */
	public String name = "";

	/**
	 * Explanation of the question.
	 */
	public String explanation = "";

	/**
	 * The qeusto to answer to.
	 */
	public String question = "";

	/**
	 * List of possible answers.
	 */
	public List<ResponseAnswer> answers = new ArrayList<>();

	/**
	 * True if this is not a real question but just an example.
	 */
	public Boolean isExample = false;

	/**
	 * If tree, the possible answers should be showed in a random order.
	 */
	public Boolean randomAnswers = false;

}
