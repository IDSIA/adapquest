package ch.idsia.adaptive.backend.persistence.responses;

import java.time.LocalDateTime;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    16.12.2020 18:09
 */
public class ResponseData {

	public String code = "";
	public String token = "";
	public LocalDateTime startTime = LocalDateTime.now();

}
