package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.NotImplementedException;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 11:54
 */
@Entity
@Data
@Accessors(chain = true)
public class Session {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Code used to generate this session and access to a survey.
	 */
	@Column(nullable = false, length = 1024)
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
	@OneToMany(mappedBy = "session")
	@OrderBy("creation asc")
	private List<State> states = new ArrayList<>();

	/**
	 * Answers given during this session.
	 */
	@OneToMany(mappedBy = "session")
	@OrderBy("creation asc")
	private Set<Answer> answers = new HashSet<>();

	/**
	 * Survey done in this session.
	 */
	@ManyToOne
	@JoinColumn(name = "fk_survey")
	private Survey survey;

	/**
	 * User that is associated with this session.
	 */
	@ManyToOne
	private User user;

	public Result getResult() {
		// TODO
		throw new NotImplementedException();
	}
}
