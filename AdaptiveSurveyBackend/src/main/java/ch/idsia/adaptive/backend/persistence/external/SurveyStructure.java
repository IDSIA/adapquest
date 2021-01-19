package ch.idsia.adaptive.backend.persistence.external;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    19.01.2021 11:58
 */
public class SurveyStructure {

	public String language = "";
	public String description = "";
	public String accessCode = "";
	public Long duration = 3600L;
	public List<String> skillOrder = new ArrayList<>();
	public Boolean randomQuestions = false;
	public Boolean mixedSkillOrder = false;
	public Boolean adaptive = false;
	public Integer questionPerSkillMin = 0;
	public Integer questionPerSkillMax = Integer.MAX_VALUE;
	public Double entropyUpperThreshold = 1.;
	public Double entropyLowerThreshold = 0.;
	public Double entropyMin = .0;
	public Integer questionValidityCheckMin = 0;
	public Integer questionTotalMin = 0;
	public Integer questionTotalMax = Integer.MAX_VALUE;

}
