package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.config.WebConfig;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    24.11.2020 17:54
 */
@RunWith(SpringRunner.class)
@WebMvcTest({
		SurveyController.class, PersistenceConfig.class, WebConfig.class,
})
class SurveyControllerTest {
//
//	@Autowired
//	private MockMvc mvc;

	@Autowired
	SurveyController surveys;

	@Test
	void countRepository() {
//		long count = surveys.surveyRepository.count();
//		System.out.println(count);
	}
}