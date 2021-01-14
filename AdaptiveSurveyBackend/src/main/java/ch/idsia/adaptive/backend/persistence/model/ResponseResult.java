package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 11:50
 */
@Data
@Accessors(chain = true)
public class ResponseResult {

	/**
	 * Number of answers given.
	 */
	private Integer answers;

	/**
	 * Total seconds.
	 */
	private Long seconds;

	/**
	 * Extra data.
	 */
	private String data;

	/**
	 * Has ended;
	 */
	private Boolean ended;

	/**
	 * Last state of the survey.
	 */
	private ResponseState state;
}
