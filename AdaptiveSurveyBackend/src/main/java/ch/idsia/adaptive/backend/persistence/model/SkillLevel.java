package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    26.11.2020 11:25
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SkillLevel implements Comparable<SkillLevel> {

	/**
	 * Human readable name of this level.
	 */
	@EqualsAndHashCode.Include
	private String name;

	/**
	 * Numeric value of this level. Used for sorting from lower to high.
	 */
	@EqualsAndHashCode.Include
	private Integer level;

	public SkillLevel(String name, Integer level) {
		this.name = name;
		this.level = level;
	}

	@Override
	public int compareTo(SkillLevel other) {
		return Integer.compare(this.level, other.level);
	}
}
