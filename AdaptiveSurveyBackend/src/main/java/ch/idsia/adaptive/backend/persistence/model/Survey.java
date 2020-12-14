package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 12:58
 */
@Entity
@Data
@Accessors(chain = true)
public class Survey {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Textual description of a survey.
	 */
	private String description;

	/**
	 * Access code for this kind of survey.
	 */
	private String accessCode;

	/**
	 * Duration in seconds of a survey.
	 */
	private Long duration;

	/**
	 * Minimum number of questions to ask for each skill.
	 */
	private Integer questionPerSkillMin = 0;

	/**
	 * Maximum number of questions to ask for each skill.
	 */
	private Integer questionPerSkillMax = Integer.MAX_VALUE;

	/**
	 * Minimum number of question to start check for the validity of a skill.
	 */
	private Integer questionValidityCheckMin = 0;

	/**
	 * Minimum number of questions to ask in total.
	 */
	private Integer questionTotalMin = 0;

	/**
	 * Maximum number of questions to ask in total.
	 */
	private Integer questionTotalMax = Integer.MAX_VALUE;

	/**
	 * Set of questions available for this Survey.
	 */
	// TODO: maybe consider
	//  (1) a pool of questions shared between surveys?
	//  (2) group of questions instead of a single question?
	@OneToMany
	@OrderBy("id ASC")
	private List<Question> questions;

	/**
	 * Survey Sessions open for this Survey.
	 */
	@OneToMany
	private Set<Session> sessions;

	@ManyToOne
	private AdaptiveModel model;
}
