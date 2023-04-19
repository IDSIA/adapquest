package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    25.11.2020 11:54
 */
@Entity
@Data
@Accessors(chain = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Session {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	/**
	 * Code used to generate this session and access to a survey.
	 */
	@Column(nullable = false, length = 1023)
	private String token;

	/**
	 * IP address of the user.
	 */
	private String remoteAddr;

	/**
	 * User agent of the user.
	 */
	private String userAgent;

	/**
	 * Access Code used to start the survey.
	 */
	private String accessCode;

	/**
	 * Field with data from Keycloak (if used).
	 */
	private String field;

	/**
	 * When this session started. This is also used as a seed for the randomness.
	 */
	private LocalDateTime startTime = LocalDateTime.now();

	/**
	 * When this session ended.
	 */
	private LocalDateTime endTime;

	/**
	 * Moment in time of the last given answer.
	 */
	private LocalDateTime lastAnswerTime;

	@Transient
	private Boolean restored = false;

	/**
	 * Statuses created during this session.
	 */
	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
	@OrderBy("creation asc")
	private List<State> states = new ArrayList<>();

	/**
	 * Answers given during this session.
	 */
	@OneToMany(mappedBy = "session", cascade = CascadeType.ALL)
	@OrderBy("creation asc")
	private Set<Answer> answers = new HashSet<>();

	/**
	 * Survey done in this session.
	 */
	@ManyToOne
	@JoinColumn(name = "fk_session_survey")
	private Survey survey;

	/**
	 * @return return the length in seconds of the survey if the {@link #endTime} i set, otherwise the number of seconds
	 * until the current date.
	 */
	public Long getElapsedSeconds() {
		LocalDateTime start = this.startTime;
		LocalDateTime end = this.endTime;

		if (this.endTime == null) {
			end = LocalDateTime.now();
		}

		return start.until(end, ChronoUnit.SECONDS);
	}
}
