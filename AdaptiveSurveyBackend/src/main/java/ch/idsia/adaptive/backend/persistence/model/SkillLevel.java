package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    26.11.2020 11:25
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SkillLevel implements Comparable<SkillLevel> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	/**
	 * Human readable name of this level.
	 */
	private String name;

	/**
	 * Numeric value of this level. Used for sorting from lower to high.
	 */
	private Double level;

	public SkillLevel(String name, Double level) {
		this.name = name;
		this.level = level;
	}

	@Override
	public int compareTo(SkillLevel other) {
		return Double.compare(this.level, other.level);
	}
}
