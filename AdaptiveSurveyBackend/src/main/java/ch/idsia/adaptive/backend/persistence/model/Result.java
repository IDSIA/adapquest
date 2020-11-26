package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 11:50
 */
@Data
public class Result {

	/**
	 * Number of answers given.
	 */
	private Integer answer;

	/**
	 * Total seconds.
	 */
	private Long seconds;

	/**
	 * Extra data.
	 */
	private String data;
}
