package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    19.01.2021 12:05
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ModelVariableStructure {

	public String name = "";
	public Integer states = 2;
	public double[] data = new double[]{.5, .5};
	public List<String> parents = new ArrayList<>();

}
