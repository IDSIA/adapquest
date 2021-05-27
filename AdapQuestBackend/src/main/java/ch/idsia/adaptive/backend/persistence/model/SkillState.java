package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    26.11.2020 11:25
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class SkillState implements Comparable<SkillState> {

	/**
	 * Human readable name of this state.
	 */
	@EqualsAndHashCode.Include
	private String name;

	/**
	 * Numeric value of this state. Used for sorting from lower to high.
	 */
	@EqualsAndHashCode.Include
	private Integer state;

	public SkillState(String name, Integer state) {
		this.name = name;
		this.state = state;
	}

	@Override
	public int compareTo(SkillState other) {
		return Integer.compare(this.state, other.state);
	}
}
