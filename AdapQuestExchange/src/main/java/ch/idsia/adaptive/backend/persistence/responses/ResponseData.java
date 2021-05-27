package ch.idsia.adaptive.backend.persistence.responses;

import java.time.LocalDateTime;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    16.12.2020 18:09
 */
public class ResponseData {

	/**
	 * Access code of the survey.
	 */
	public String code = "";

	/**
	 * Token to use for the current session.
	 */
	public String token = "";

	/**
	 * When the session started.
	 */
	public LocalDateTime startTime = LocalDateTime.now();

}
