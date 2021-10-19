package ch.idsia.adaptive.experiments.models;

import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.external.QuestionStructure;
import ch.idsia.adaptive.backend.persistence.external.SkillStructure;
import ch.idsia.adaptive.backend.persistence.external.SurveyStructure;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    01.02.2021 09:24
 * <p>
 * This is just a simple abstract class that can be used to implement with code an adaptive survey compatible with the
 * remote application.
 */
public abstract class AbstractAdaptiveModel {
	private static final Logger logger = LoggerFactory.getLogger(AbstractAdaptiveModel.class);

	/**
	 * Above this threshold, stop query the skill.
	 */
	public Double ENTROPY_STOP_THRESHOLD_MAX = 1.;
	/**
	 * Below this threshold, stop query the skill.
	 */
	public Double ENTROPY_STOP_THRESHOLD_MIN = .25;

	String accessCode;

	public String getAccessCode() {
		return accessCode;
	}

	/**
	 * @param accessCode code used to access to this survey on the remote application.
	 */
	public AbstractAdaptiveModel(String accessCode) {
		this.accessCode = accessCode;
	}

	/**
	 * Definition of the model. Overwrite this method and define your model before serialize it in a {@link String}. Use
	 * {@link BayesUAIWriter#serialize()} to correctly serialize the model.
	 *
	 * @return a serialized model saved in a {@link String} object.
	 */
	public abstract String model();

	/**
	 * Definition of the skills. Override this method by filling in your skills.
	 *
	 * @return a list of {@link SkillStructure}s.
	 */
	public abstract List<SkillStructure> skills();

	/**
	 * Definition of the questions. Override this method by filling it with your questions.
	 *
	 * @return a list of {@link QuestionStructure}s.
	 */
	public abstract List<QuestionStructure> questions();

	/**
	 * Definition of a survey control object. This is a minimal implementation of a valid {@link SurveyStructure}.
	 * Ideally, when you override this method, you first call this via super to be sure that the minimal parameters are
	 * correctly set.
	 *
	 * @return a valid {@link SurveyStructure}
	 */
	public SurveyStructure survey() {
		return new SurveyStructure()
				.setAccessCode(accessCode)
				.setScoreLowerThreshold(ENTROPY_STOP_THRESHOLD_MIN)
				.setScoreUpperThreshold(ENTROPY_STOP_THRESHOLD_MAX);
	}

	/**
	 * This method will build a {@link ImportStructure} by calling, in order, the methods {@link #model()},
	 * {@link #skills()}, {@link #questions()}, and {@link #survey()}. Optimally, this method does not need to be
	 * overwritten.
	 *
	 * @return a valid {@link ImportStructure} object.
	 */
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
