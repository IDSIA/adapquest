package ch.idsia.adaptive.backend.persistence.external;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    19.01.2021 12:01
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class StateStructure {

	/**
	 * Name of the state.
	 */
	public String name = "";

	/**
	 * Index of the state in the variable.
	 */
	public Integer value = 0;

}
