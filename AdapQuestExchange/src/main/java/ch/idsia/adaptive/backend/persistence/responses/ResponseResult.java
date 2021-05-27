package ch.idsia.adaptive.backend.persistence.responses;


import java.time.LocalDateTime;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    25.11.2020 11:50
 */
public class ResponseResult {

	/**
	 * Total seconds.
	 */
	public Long seconds;

	/**
	 * Extra data.
	 */
	public LocalDateTime data;

	/**
	 * Has ended;
	 */
	public Boolean ended;

	/**
	 * Last state of the survey.
	 */
	public ResponseState state;

}
