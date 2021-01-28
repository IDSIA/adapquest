package ch.idsia.adaptive.backend.persistence.external;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    19.01.2021 12:02
 */
public class ImportStructure {

	public SurveyStructure survey = null;
	public List<SkillStructure> skills = new ArrayList<>();
	public List<QuestionStructure> questions = new ArrayList<>();
	public ModelStructure model;

}
