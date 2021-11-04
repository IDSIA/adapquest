package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.utils.ListSkillStateConverter;
import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    25.11.2020 11:51
 * <p>
 *
 * </p>
 */
@Entity
@Data
@Accessors(chain = true)
@EqualsAndHashCode()
public class Skill implements Comparable<Skill> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	/**
	 * Skill name.
	 */
	private String name;

	/**
	 * States of this skill. They are equals to the states of the variable in the model.
	 */
	@Convert(converter = ListSkillStateConverter.class)
	@Column(name = "states", length = 1023)
	private List<SkillState> states;

	/**
	 * Index of the variable in the model.
	 */
	private Integer variable;

	/**
	 * Survey that include this question.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_skill_survey")
	@JsonBackReference
	private Survey survey;

	@Override
	public String toString() {
		return "Skill{'" + name + "'}";
	}

	@Override
	public int compareTo(Skill other) {
		return Integer.compare(this.variable, other.variable);
	}
}
