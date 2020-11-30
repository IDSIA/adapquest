package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.config.WebConfig;
import ch.idsia.adaptive.backend.controller.SurveyController;
import ch.idsia.adaptive.backend.persistence.dao.QuestionRepository;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.services.SessionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    24.11.2020 17:54
 */
@RunWith(SpringRunner.class)
@WebMvcTest({
		WebConfig.class,
		PersistenceConfig.class,
		SurveyRepository.class,
		QuestionRepository.class,
		SurveyController.class,
		SessionService.class,
})
@Transactional
class TestSurveyFlow {

	String accessCode = "Code123";

	@Autowired
	ObjectMapper om;

	@Autowired
	MockMvc mvc;

	@Autowired
	SurveyRepository surveys;

	@Autowired
	QuestionRepository questions;

	@Autowired
	DataSource dataSource;

	@BeforeEach
	void setUp() {
		// single skill
		Skill skill = new Skill()
				.setName("A")
				.setLevels(List.of(
						new SkillLevel("low", 0.0),
						new SkillLevel("high", 1.0))
				);

		// 3 questions
		Question q1 = new Question()
				.setSkill(skill)
				.setAnswersAvailable(List.of(
						new QuestionAnswer("a"),
						new QuestionAnswer("b"),
						new QuestionAnswer("c", true),
						new QuestionAnswer("d")
				));
		Question q2 = new Question()
				.setSkill(skill)
				.setAnswersAvailable(List.of(
						new QuestionAnswer("a"),
						new QuestionAnswer("b"),
						new QuestionAnswer("c", true),
						new QuestionAnswer("d")
				));
		Question q3 = new Question()
				.setSkill(skill)
				.setAnswersAvailable(List.of(
						new QuestionAnswer("a"),
						new QuestionAnswer("b"),
						new QuestionAnswer("c", true),
						new QuestionAnswer("d")
				));

		questions.saveAll(List.of(q1, q2, q3));

		// create new survey
		Survey survey = new Survey()
				.setAccessCode(accessCode)
				.setDescription("This is just a description")
				.setDuration(3600L)
				.setQuestions(Set.of(q1, q2, q3));

		surveys.save(survey);

		System.out.println(survey);
	}

	@Test
	void defaultFlow() throws Exception {
		// use access code to register, init a survey, and get personal the access token
		MvcResult result = mvc.perform(get("/survey/init").param("accessCode", this.accessCode))
				.andExpect(status().isOk())
				.andReturn();

		SurveyData data = om.readValue(result.getResponse().getContentAsString(), SurveyData.class);

		Assertions.assertNotNull(data.getToken(), "Session token is null");
		Assertions.assertEquals(accessCode, data.getAccessCode(), "Access codes are different");

		// get current state of the skills
		mvc.perform(get("/survey/state")).andExpect(status().isOk());

		// get next question
		mvc.perform(get("/survey/next")).andExpect(status().isOk());

		// post answer
		mvc.perform(post("/survey/answer")).andExpect(status().isOk());

		// get last status
		mvc.perform(get("/survey/status")).andExpect(status().isOk());
	}

	@Test
	void wrongAccessCode() throws Exception {
		// TODO
		throw new NotImplementedException();
	}

	@Test
	void wrongSessionToken() throws Exception {
		// TODO
		throw new NotImplementedException();
	}


}