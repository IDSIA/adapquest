package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    28.01.2021 12:15
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ModelStructure {

	public List<ModelVariableStructure> variables = new ArrayList<>();

}
