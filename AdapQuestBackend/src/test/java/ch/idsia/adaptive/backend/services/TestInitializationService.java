package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.SurveyStructureRepository;
import ch.idsia.adaptive.backend.TestApplication;
import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    19.01.2021 13:12
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestApplication.class)
@WebMvcTest({
		TestApplication.class,
		PersistenceConfig.class,
		SurveyRepository.class,
		InitializationService.class,
})
public class TestInitializationService {

	@Autowired
	InitializationService is;

	@Autowired
	SurveyRepository surveys;

	@Test
	public void testLoadFromDataFolder() {
		final File[] files = Paths.get("", "data", "surveys").toFile().listFiles();
		final int n = files == null ? 0 : files.length -1;
		final long count = surveys.count();

		is.readDataSurveysFolder();

		assertEquals(count + n, surveys.count());

		final Survey survey = surveys.findByAccessCode("NonAdapQuest-Example");

		assertNotNull(survey);
		assertEquals(1, survey.getSkillOrder().size());
		assertEquals(3, survey.getQuestions().size());

		final List<Question> qs = new ArrayList<>(survey.getQuestions());
		qs.sort(Comparator.comparingInt(Question::getVariable));

		assertEquals(3, qs.get(0).getAnswersAvailable().size());
		assertEquals(2, qs.get(1).getAnswersAvailable().size());
		assertEquals(3, qs.get(2).getAnswersAvailable().size());

		List<String> lines = Arrays.stream(survey.getModelData().split("\n")).collect(Collectors.toList());

		BayesianNetwork bn = new BayesUAIParser(lines).parse();

		assertEquals(4, bn.getVariables().length);
	}

	@Test
	public void testLoadFromStructureWithModelData() {
		final ImportStructure structure = SurveyStructureRepository.structure2S2Q("test");
		is.parseSurvey(structure);

		final Survey survey = surveys.findByAccessCode("test");
		assertNotNull(survey);
		survey.getQuestions().forEach(q -> {
					assertTrue(q.getVariable() >= 0);
					q.getSkills().forEach(s -> assertTrue(s.getVariable() >= 0));
				}
		);
	}
}