package ch.idsia.adaptive.experiments.kitt4sme;

import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.model.QuestionAnswer;
import ch.idsia.adaptive.backend.persistence.model.Skill;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.InitializationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    18.10.2021 10:12
 */
public class InitSurvey {

	public static Survey init(String filename) throws Exception {
		final ObjectMapper om = new ObjectMapper();
		final ImportStructure structure = om.readValue(new File(filename), ImportStructure.class);
		final Survey survey = InitializationService.parseSurveyStructure(structure);

		// this is just a fix to have ids also without a database, for experimenting purposes
		for (Skill skill : survey.getSkills()) {
			skill.setId(skill.getVariable() + 1L);
		}

		survey.getQuestions()
				.forEach(q -> {
					q.setId(Long.parseLong(q.getName().substring(1)));

					for (int i = 0; i < q.getAnswersAvailable().size(); i++) {
						final QuestionAnswer qa = q.getAnswersAvailable().get(i);
						qa.setId(q.getId() * 100 + i + 1L);
					}
				});

		return survey;
	}
}
