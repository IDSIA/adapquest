package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    19.01.2021 12:00
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class SkillStructure {

	/**
	 * Name of the skill.
	 */
	public String name = "";

	/**
	 * Index variable in the model.
	 */
	public Integer variable = -1;

	/**
	 * Ordred list of states.
	 */
	public List<StateStructure> states = new ArrayList<>();

}
