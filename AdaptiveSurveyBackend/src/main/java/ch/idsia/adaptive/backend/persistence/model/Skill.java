package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.utils.ListSkillLevelConverter;
import lombok.Data;
import lombok.EqualsAndHashCode;
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
@EqualsAndHashCode(onlyExplicitlyIncluded = false)
public class Skill implements Comparable<Skill> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	/**
	 * Skill name;
	 */
	private String name;

	/**
	 * Level grades of this skill. They are equals to the states of the variable in the model.
	 */
	@Convert(converter = ListSkillLevelConverter.class)
	@Column(name = "levels", length = 1023)
	private List<SkillLevel> states;

	/**
	 * Index of the variable in the model.
	 */
	private Integer variable;

	@Override
	public String toString() {
		return "Skill{" +
				"id=" + id +
				", name='" + name + '\'' +
				'}';
	}

	@Override
	public int compareTo(Skill other) {
		return Integer.compare(this.variable, other.variable);
	}
}
