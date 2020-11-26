package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.utils.ListStringConverter;
import lombok.Data;

import javax.persistence.*;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    26.11.2020 11:08
 */
@Entity
@Data
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
	private String path;

	/**
	 * Order of the skill in the model skill-chain.
	 */
	// TODO: maybe this is model fixed?
	@Transient
	@Convert(converter = ListStringConverter.class)
	private List<String> skillOrder;

	/**
	 * If ture questions can be asked with a random order, otherwise it will follow the skillOrder field.
	 */
	private Boolean mixedSkillOrder = true;

	/**
	 * If true, this model follow an adaptive schema.
	 */
	private Boolean isAdaptive = true;

	/**
	 * Minimum number of questions to be asked for each skill.
	 */
	private Integer questionPerSkillMin;

	/**
	 * Maximum number of questions that can be asked for each skill.
	 */
	private Integer questionPerSkillMax;

	/**
	 * Entrpy threshold for early stop.
	 */
	private Integer entropyMin;

}
