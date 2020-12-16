package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.utils.ListSkillLevelConverter;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 11:51
 * <p>
 *
 * </p>
 */
@Entity
@Data
@Accessors(chain = true)
public class Skill {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Skill name;
	 */
	private String name;

	/**
	 * Level grades of this skill.
	 */
	@Transient
	@Convert(converter = ListSkillLevelConverter.class)
	private List<SkillLevel> levels;

	/**
	 * Questions assigned to this skill.
	 */
	@OneToMany
	@OrderBy("id ASC")
	private List<Question> questions;
}
