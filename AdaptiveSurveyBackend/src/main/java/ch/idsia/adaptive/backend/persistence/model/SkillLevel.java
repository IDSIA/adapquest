package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    26.11.2020 11:25
 */
@Entity
@Data
public class SkillLevel implements Comparable<SkillLevel> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Human readable name of this level.
	 */
	private String name;

	/**
	 * Numeric value of this level. Used for sorting from lower to high.
	 */
	private Double level;

	@Override
	public int compareTo(SkillLevel other) {
		return Double.compare(this.level, other.level);
	}
}
