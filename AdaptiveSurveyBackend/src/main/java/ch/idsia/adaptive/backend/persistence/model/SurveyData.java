package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 16:56
 */
@Data
@Accessors(chain = true)
public class SurveyData {

	private Long id;

	private String accessCode;
	private String token; // this is a unique id and is NOT the access token!

	private String remoteAddress;
	private String userAgent;
	private LocalDateTime startTime;

	private Long survey;
	private Long question;

	public void setFromSession(Session session) {
		setToken(session.getToken());
		setStartTime(session.getStartTime());
		setSurvey(session.getSurvey().getId());
		setQuestion(-1L);
	}
}
