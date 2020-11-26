package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import org.apache.commons.lang3.NotImplementedException;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 11:54
 */
@Entity
@Data
public class Session {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Code used to generate this session and access to a survey.
	 */
	@Column(nullable = false)
	private String token = UUID.randomUUID().toString();

	/**
	 * IP address of the user.
	 */
	private String remoteAddr;

	/**
	 * User agent of the user.
	 */
	private String userAgent;

	/**
	 * When this session started.
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
	@OneToMany
	@OrderBy("creation asc")
	private List<Status> statuses = new ArrayList<>();

	/**
	 * Answers given during this session.
	 */
	@OneToMany
	@OrderBy("creation asc")
	private Set<Answer> answers = new HashSet<>();

	/**
	 * Survey done in this session.
	 */
	@ManyToOne
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
