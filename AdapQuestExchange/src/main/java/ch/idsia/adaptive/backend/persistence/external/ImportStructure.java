package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    19.01.2021 12:02
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ImportStructure {

	/**
	 * Definition of survey.
	 */
	public SurveyStructure survey = null;

	/**
	 * Definition of skills.
	 */
	public List<SkillStructure> skills = new ArrayList<>();

	/**
	 * Definition of available questions.
	 */
	public List<QuestionStructure> questions = new ArrayList<>();

	/**
	 * Definition of the structure of a Bayesian model. This will be converted in a real {@link ch.idsia.crema.model.graphical.BayesianNetwork}.
	 */
	public ModelStructure model = null;

	/**
	 * Definition of a structure of a Bayesian model. This is a serialized version of a {@link ch.idsia.crema.model.graphical.BayesianNetwork}
	 * and has a priority over the {@link #model} field (i.e.: if both are defined, the system will consider only the
	 * string stored in this field).
	 */
	public String modelData = null; // has priority

}
