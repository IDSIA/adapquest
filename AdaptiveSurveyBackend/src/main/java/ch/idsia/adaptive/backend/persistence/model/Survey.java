package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.utils.ListStringConverter;
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

	// TODO: maybe set active days from-to?

	/**
	 * Model to load from disk.
	 */
	private String modelData;

	/**
	 * Skills used in the survey.
	 */
	@OneToMany(fetch = FetchType.EAGER)
	private Set<Skill> skills;

	/**
	 * Order of the skill in the model skill-chain.
	 */
	// TODO: maybe this is model fixed?
	@Transient
	@Convert(converter = ListStringConverter.class)
	private List<String> skillOrder;

	/**
	 * If true, the questions will be chosen randomly.
	 */
	private Boolean questionsAreRandom = false;

	/**
	 * If true questions can be asked with a random order, otherwise it will follow the skillOrder field.
	 */
	private Boolean mixedSkillOrder = true;

	/**
	 * If true, this model follows an adaptive schema.
	 */
	private Boolean isAdaptive = true;

	/**
	 * Minimum number of questions to be asked for each skill.
	 */
	private Integer questionPerSkillMin = 0;

	/**
	 * Maximum number of questions that can be asked for each skill.
	 */
	private Integer questionPerSkillMax = Integer.MAX_VALUE;

	/**
	 * Stop the skill query when the entropy of the model is above this threshold.
	 */
	private Double entropyUpperThreshold = 1.0;

	/**
	 * Stop the skill query when the entropy of the model is below this threshold.
	 */
	private Double entropyLowerThreshold = 0.0;

	/**
	 * Entropy threshold for early stop.
	 */
	private Double entropyMin;

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
	@OneToMany(mappedBy = "survey", fetch = FetchType.EAGER)
	@OrderBy("id ASC")
	private List<Question> questions;

	/**
	 * Survey Sessions open for this Survey.
	 */
	@OneToMany(fetch = FetchType.EAGER)
	private Set<Session> sessions;

}
