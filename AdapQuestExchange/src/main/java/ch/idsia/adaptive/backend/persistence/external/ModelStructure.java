package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    28.01.2021 12:15
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ModelStructure {

	/**
	 * A model is defined as a list of variables, possibly ordered by existence.
	 */
	public List<ModelVariableStructure> variables = new ArrayList<>();

}
