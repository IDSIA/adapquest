package ch.idsia.adaptive.experiments.models;

import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.external.QuestionStructure;
import ch.idsia.adaptive.backend.persistence.external.SkillStructure;
import ch.idsia.adaptive.backend.persistence.external.SurveyStructure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    01.02.2021 09:24
 */
public abstract class AbstractAdaptiveModel {
	private static final Logger logger = LogManager.getLogger(AbstractAdaptiveModel.class);

	public Double ENTROPY_STOP_THRESHOLD_MAX = .0;
	public Double ENTROPY_STOP_THRESHOLD_MIN = .25;

	String accessCode;

	public AbstractAdaptiveModel(String accessCode) {
		this.accessCode = accessCode;
	}

	public abstract String model();

	public abstract List<SkillStructure> skills();

	public abstract List<QuestionStructure> questions();

	public SurveyStructure survey() {
		return new SurveyStructure()
				.setAccessCode(accessCode)
				.setEntropyLowerThreshold(ENTROPY_STOP_THRESHOLD_MIN)
				.setEntropyUpperThreshold(ENTROPY_STOP_THRESHOLD_MAX);
	}

	public ImportStructure structure() {
		final String modelData = model();
		final List<SkillStructure> skills = skills();
		final List<QuestionStructure> questions = questions();
		final SurveyStructure survey = survey();

		logger.info("Created new survey with accessCode={}", accessCode);
		return new ImportStructure()
				.setSurvey(survey)
				.setSkills(skills)
				.setQuestions(questions)
				.setModelData(modelData);
	}

}
