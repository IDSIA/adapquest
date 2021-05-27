package ch.idsia.adaptive.backend.persistence.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    29.01.2021 15:03
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RequestAnswer {

	/**
	 * Id of the question to answer to.
	 */
	public Long question;

	/**
	 * Id of the choosen answer.
	 */
	public Long answer;

}
