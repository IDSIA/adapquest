package ch.idsia.adaptive.backend.persistence.responses;

import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    29.01.2021 15:58
 */
public class ResponseSkill {

	/**
	 * Name of the skill.
	 */
	public String name;

	/**
	 * List of states of this skill.
	 */
	public List<ResponseSkillState> states;

	/**
	 * Index of the variable in the model.
	 */
	public Integer variable;

}
