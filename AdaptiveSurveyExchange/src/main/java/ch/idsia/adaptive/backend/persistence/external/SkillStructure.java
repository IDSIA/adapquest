package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    19.01.2021 12:00
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class SkillStructure {

	public String name = "";
	public Integer variable = -1;
	public List<StateStructure> states = new ArrayList<>();

}
