package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    28.01.2021 09:57
 */
@Entity
@Data
@Accessors(chain = true)
public class Client {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * User identifier of this client.
	 */
	private String username;

	/**
	 * Contact information of the owner of this client.
	 */
	private String email;

	/**
	 * Key to use in the header of each request to the {@link ch.idsia.adaptive.backend.controller.ConsoleController} backend.
	 */
	private String key;

}
