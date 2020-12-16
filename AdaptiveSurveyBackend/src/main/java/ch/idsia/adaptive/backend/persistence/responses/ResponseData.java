package ch.idsia.adaptive.backend.persistence.responses;

import ch.idsia.adaptive.backend.persistence.model.SurveyData;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    16.12.2020 18:09
 */
@NoArgsConstructor
public class ResponseData {

	public String code = "";
	public String token = "";
	public LocalDateTime startTime = LocalDateTime.now();

	public ResponseData(SurveyData data) {
		this.code = data.getAccessCode();
		this.token = data.getToken();
		this.startTime = data.getStartTime();
	}
}
