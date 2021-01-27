package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.utils.ListStringConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Survey {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	/**
	 * Language code of this survey.
	 */
	private String language;

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
	@Column(length = 1048575) // 1MB
	private String modelData;

	/**
	 * Order of the skill in the model skill-chain.
	 */
	@Convert(converter = ListStringConverter.class)
	@Column(name = "skillOrder", length = 1023)
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
	 * Stop the skill query when the entropy of the node is above this threshold.
	 */
	private Double entropyUpperThreshold = 1.0;

	/**
	 * Stop the skill query when the entropy of the node is below this threshold.
	 */
	private Double entropyLowerThreshold = 0.0;

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
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	@OrderBy("id ASC")
	private List<Question> questions;

	/**
	 * Survey Sessions open for this Survey.
	 */
	@OneToMany(mappedBy = "survey", cascade = CascadeType.PERSIST, fetch = FetchType.LAZY)
	private Set<Session> sessions;

}
