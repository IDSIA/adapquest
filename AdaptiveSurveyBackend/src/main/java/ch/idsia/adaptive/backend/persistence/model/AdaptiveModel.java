package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.utils.ListStringConverter;
import ch.idsia.adaptive.backend.persistence.utils.MapStringIntegerConverter;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    26.11.2020 11:08
 */
@Entity
@Data
@Accessors(chain = true)
public class AdaptiveModel {

	/*
	TODO: maybe assign this model to a session instead of a survey?
	      In this way we can have different surveys with different models but with the same question pool
	*/

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Model to load from disk.
	 */
	private String data;

	/**
	 * Order of the skill in the model skill-chain.
	 */
	// TODO: maybe this is model fixed?
	@Transient
	@Convert(converter = ListStringConverter.class)
	private List<String> skillOrder;

	@Transient
	@Convert(converter = MapStringIntegerConverter.class)
	private Map<String, Integer> skillToVariable;

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

	@OneToMany(cascade = CascadeType.ALL)
	private Set<Survey> surveys;

}
