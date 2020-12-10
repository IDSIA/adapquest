package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

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
	@OneToMany(cascade = CascadeType.ALL)
	@OrderBy("level ASC")
	private List<SkillLevel> levels;

	/**
	 * Questions assigned to this skill.
	 */
	@OneToMany
	private Set<Question> questions;
}
