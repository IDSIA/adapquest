package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.TestApplication;
import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIParser;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    19.01.2021 13:12
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		TestApplication.class,
		PersistenceConfig.class,
		SurveyRepository.class,
		InitializationService.class,
})
class InitializationServiceTest {

	@Autowired
	InitializationService is;

	@Autowired
	SurveyRepository surveys;

	@Test
	public void testLoadFromDataFolder() {

		is.readDataFolder();

		assertEquals(2, surveys.count());

		final Survey survey = surveys.findByAccessCode("NonAdaptiveSurvey-Example");

		assertNotNull(survey);
		assertEquals(1, survey.getSkillOrder().size());
		assertEquals(3, survey.getQuestions().size());

		assertEquals(3, survey.getQuestions().get(0).getAnswersAvailable().size());
		assertEquals(2, survey.getQuestions().get(1).getAnswersAvailable().size());
		assertEquals(3, survey.getQuestions().get(2).getAnswersAvailable().size());

		List<String> lines = Arrays.stream(survey.getModelData().split("\n")).collect(Collectors.toList());

		BayesianNetwork bn = new BayesUAIParser(lines).parse();

		assertEquals(4, bn.getVariables().length);
	}

}