package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.AdaptiveSurveyBackend;
import ch.idsia.adaptive.backend.SurveyStructureRepository;
import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.config.WebConfig;
import ch.idsia.adaptive.backend.persistence.dao.*;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.requests.RequestCode;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.backend.services.InitializationService;
import ch.idsia.adaptive.backend.services.SessionService;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.transaction.Transactional;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
		AnswerRepository.class,
		QuestionAnswerRepository.class,
		SessionRepository.class,
		SurveyRepository.class,
		StatesRepository.class,
		SessionService.class,
		SurveyManagerService.class,
		InitializationService.class,
		ConsoleController.class,
		SurveyController.class,
})
@Transactional
class ConsoleControllerSurveyTest {

	@Autowired
	ObjectMapper om;

	@Autowired
	MockMvc mvc;

	@Autowired
	SessionRepository sessions;
	@Autowired
	StatesRepository states;
	@Autowired
	SurveyRepository surveys;
	@Autowired
	QuestionRepository questions;
	@Autowired
	AnswerRepository answers;
	@Autowired
	QuestionAnswerRepository questionAnswers;

	final String code = "test";
	final String key = "test";

	String token;

	@BeforeEach
	void setUp() throws Exception {
		surveys.deleteAll();

		final ImportStructure structure = SurveyStructureRepository.structure2Q2S(code);
		mvc
				.perform(post("/console/survey")
						.header("APIKey", key)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(om.writeValueAsString(structure))
				).andExpect(status().isCreated());

		MvcResult result = mvc
				.perform(get("/survey/init/" + code))
				.andExpect(status().isOk())
				.andReturn();

		ResponseData data = om.readValue(result.getResponse().getContentAsString(), ResponseData.class);
		token = data.token;

		result = mvc
				.perform(get("/survey/question/" + token))
				.andExpect(status().isOk())
				.andReturn();

		ResponseQuestion question = om.readValue(result.getResponse().getContentAsString(), ResponseQuestion.class);

		mvc
				.perform(post("/survey/answer/" + token)
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("question", "" + question.id)
						.param("answer", "" + question.answers.get(0).id)
				).andExpect(status().isOk());
	}

	@Test
	public void getAllStatesForSurvey() throws Exception {
		MvcResult result = mvc
				.perform(get("/console/survey/states/" + code)
						.header("APIKey", key)
						.contentType(MediaType.ALL_VALUE)
				).andExpect(status().isOk())
				.andReturn();

		TypeReference<List<ResponseState>> t = new TypeReference<>() {
		};

		List<ResponseState> rs = om.readValue(result.getResponse().getContentAsString(), t);

		Assertions.assertEquals(1, sessions.count());
		Assertions.assertEquals(states.count(), rs.size());
	}

	@Test
	public void removeSurvey() throws Exception {
		mvc
				.perform(delete("/console/survey")
						.header("APIKey", key)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(om.writeValueAsString(new RequestCode(code)))
				).andExpect(status().isOk())
				.andReturn();


		Assertions.assertEquals(0, sessions.count());
		Assertions.assertEquals(0, states.count());
		Assertions.assertEquals(0, questions.count());
		Assertions.assertEquals(0, answers.count());
		Assertions.assertEquals(0, questionAnswers.count());
		Assertions.assertEquals(0, surveys.count());
	}

	@Test
	public void cleanSurvey() throws Exception {
		mvc
				.perform(delete("/console/survey/clean")
						.header("APIKey", key)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(om.writeValueAsString(new RequestCode(code)))
				).andExpect(status().isOk())
				.andReturn();


		Assertions.assertEquals(0, sessions.count());
		Assertions.assertEquals(0, states.count());
		Assertions.assertEquals(0, answers.count());
		Assertions.assertEquals(4, questionAnswers.count());
		Assertions.assertEquals(2, questions.count());
		Assertions.assertEquals(1, surveys.count());
	}

}