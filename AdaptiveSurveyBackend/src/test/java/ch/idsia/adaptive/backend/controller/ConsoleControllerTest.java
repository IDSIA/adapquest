package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.AdaptiveSurveyBackend;
import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.config.WebConfig;
import ch.idsia.adaptive.backend.persistence.dao.ClientRepository;
import ch.idsia.adaptive.backend.persistence.dao.SessionRepository;
import ch.idsia.adaptive.backend.persistence.dao.StatesRepository;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.external.SurveyStructure;
import ch.idsia.adaptive.backend.services.InitializationService;
import ch.idsia.adaptive.backend.utils.TestTool;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    29.01.2021 19:46
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = AdaptiveSurveyBackend.class)
@WebMvcTest({
		WebConfig.class,
		PersistenceConfig.class,
		ClientRepository.class,
		SessionRepository.class,
		SurveyRepository.class,
		StatesRepository.class,
		InitializationService.class,
		ConsoleController.class,
})
@Import(TestTool.class)
class ConsoleControllerTest {

	@Autowired
	TestTool tool;

	@Test
	public void testPostNewSurveyStructure() throws Exception {
		ImportStructure is = new ImportStructure()
				.setSurvey(new SurveyStructure());

		tool.consoleSurveyAdd("test", is);
	}
}