package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    19.01.2021 11:58
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class SurveyStructure {

	/**
	 * Defines the language of the survey.
	 */
	public String language = "";

	/**
	 * Defines a description of the survey.
	 */
	public String description = "";

	/**
	 * Define the access code of this survey. This is a mandatory field.
	 */
	public String accessCode = "";

	/**
	 * Length in seconds of the survey.
	 */
	public Long duration = 3600L;

	/**
	 * Order in which the skills' questions need to appear in the survey.
	 */
	public List<String> skillOrder = new ArrayList<>();

	/**
	 * If true, questions are randomly.
	 */
	public Boolean randomQuestions = false;

	/**
	 * False if {@link #skillOrder} is set, otherwise true.
	 */
	public Boolean mixedSkillOrder = false;

	/**
	 * If true, the survey is adaptive. Otherwise it is not.
	 */
	public Boolean adaptive = false;

	/**
	 * Minimum number of question for each skill.
	 */
	public Integer questionPerSkillMin = 0;

	/**
	 * Maximum number of question for each skill.
	 */
	public Integer questionPerSkillMax = Integer.MAX_VALUE;

	/**
	 * Upper threshold for skill entropy. If the entropy for the skill is above this threshold, the skill is considered completed.
	 */
	public Double entropyUpperThreshold = 1.;

	/**
	 * Lower threshold for skill entropy. If the entropy for the skill is below this threshold, the skill is considered completed.
	 */
	public Double entropyLowerThreshold = 0.;

	/**
	 * Upper threshold for mean entropy of all skills. If the value is above this threshold, the survey is considered completed.
	 */
	public Double globalMeanEntropyUpperThreshold = 1.;

	/**
	 * Lower threshold for mean entropy of all skills. If the value is below this threshold, the survey is considered completed.
	 */
	public Double globalMeanEntropyLowerThreshold = 0.;

	/**
	 * Minimum number of question to start check for the validity of a skill.
	 */
	public Integer questionValidityCheckMin = 0;

	/**
	 * Minimum number of question to pose, independent of the skills.
	 */
	public Integer questionTotalMin = 0;

	/**
	 * Maximum number of question to pose, independent of the skills.
	 */
	public Integer questionTotalMax = Integer.MAX_VALUE;

}
