package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 13:05
 */
@Entity
@Data
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Username assigned to a user.
	 */
	private String username;

	/**
	 * When the account was created.
	 */
	private LocalDateTime registrationTime;

	/**
	 * Sessions opened by this user.
	 */
	@OneToMany
	private Set<Session> sessions;

}
