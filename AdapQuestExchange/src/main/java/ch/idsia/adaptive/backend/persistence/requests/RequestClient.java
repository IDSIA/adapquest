package ch.idsia.adaptive.backend.persistence.requests;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    29.01.2021 14:38
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class RequestClient {

	/**
	 * Used to track who requested a key.
	 */
	public String username;

	/**
	 * Used to contact who requested a key.
	 */
	public String email;

}
