package ch.idsia.adaptive.backend.persistence.requests;

import lombok.Data;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    29.01.2021 15:03
 */
@Data
public class RequestAnswer {

	public Long question;
	public Long answer;

}
