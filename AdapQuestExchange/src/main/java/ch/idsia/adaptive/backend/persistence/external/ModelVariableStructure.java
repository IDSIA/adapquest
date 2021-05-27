package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    19.01.2021 12:05
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ModelVariableStructure {

	/**
	 * Name of this variable.
	 */
	public String name = "";

	/**
	 * Number of states of this variable.
	 */
	public Integer states = 2;

	/**
	 * Conditional Probability Table assigned to this variable.
	 */
	public double[] data = new double[]{.5, .5};

	/**
	 * Name of the parent variables (they must appear before this declaration in {@link ModelStructure#variables}.
	 */
	public List<String> parents = new ArrayList<>();

}
