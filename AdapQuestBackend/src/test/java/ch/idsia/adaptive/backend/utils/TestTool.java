package ch.idsia.adaptive.backend.utils;

import ch.idsia.adaptive.backend.controller.ConsoleController;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.requests.RequestCode;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    02.02.2021 07:55
 */
@TestComponent
public class TestTool {
	private static final Logger logger = LoggerFactory.getLogger(ConsoleController.class);

	ObjectMapper om;
	MockMvc mvc;

	@Autowired
	public TestTool(ObjectMapper om, MockMvc mvc) {
		this.om = om;
		this.mvc = mvc;
	}

	public void consoleSurveyAdd(String key, ImportStructure structure) throws Exception {
		logger.info("Console: add new survey");
		mvc
				.perform(post("/console/survey")
						.header("APIKey", key)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(om.writeValueAsString(structure))
				).andExpect(status().isCreated());
	}

	public void consoleSurveyRemove(String key, String code) throws Exception {
		logger.info("Console: remove survey with code={}", code);
		mvc
				.perform(delete("/console/survey")
						.header("APIKey", key)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(om.writeValueAsString(new RequestCode(code)))
				).andExpect(status().isOk())
				.andReturn();
	}

	public void consoleSurveyClean(String key, String code) throws Exception {
		logger.info("Console: clean survey with code={}", code);
		mvc
				.perform(delete("/console/survey/clean")
						.header("APIKey", key)
						.contentType(MediaType.APPLICATION_JSON_VALUE)
						.content(om.writeValueAsString(new RequestCode(code)))
				).andExpect(status().isOk())
				.andReturn();
	}

	public List<ResponseState> consoleStates(String key, String code) throws Exception {
		logger.info("Console: get states for survey with code={}", code);
		MvcResult result = mvc
				.perform(get("/console/survey/states/" + code)
						.header("APIKey", key)
						.contentType(MediaType.ALL_VALUE)
				).andExpect(status().isOk())
				.andReturn();

		TypeReference<List<ResponseState>> t = new TypeReference<>() {
		};

		return om.readValue(result.getResponse().getContentAsString(), t);
	}

	public ResponseData init(String code) throws Exception {
		logger.info("Survey: init survey with code={}", code);
		MvcResult result;
		result = mvc
				.perform(get("/survey/init/" + code))
				.andExpect(status().isOk())
				.andReturn();

		return om.readValue(result.getResponse().getContentAsString(), ResponseData.class);
	}

	public ResponseQuestion next(String token) throws Exception {
		logger.info("Survey: next question for token={}", token);
		MvcResult result = mvc
				.perform(get("/survey/question/" + token))
				.andExpect(status().is2xxSuccessful())
				.andReturn();

		if (result.getResponse().getStatus() == 204) {
			return null;
		}


		return om.readValue(result.getResponse().getContentAsString(), ResponseQuestion.class);
	}

	public void answer(String token, Long questionId, Long... answerId) throws Exception {
		logger.info("Survey: answer=({}, {}) for token={}", questionId, answerId, token);
		final MockHttpServletRequestBuilder req = post("/survey/answer/" + token)
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("question", "" + questionId);

		for (Long aid : answerId) {
			req.param("answers", "" + aid);
		}

		mvc.perform(req).andExpect(status().isOk());
	}

	public ResponseState state(String token) throws Exception {
		logger.info("Survey: last state for token={}", token);
		MvcResult result = mvc
				.perform(get("/survey/state/" + token))
				.andExpect(status().isOk())
				.andReturn();

		return om.readValue(result.getResponse().getContentAsString(), ResponseState.class);
	}

	public List<ResponseState> states(String token) throws Exception {
		logger.info("Survey: all states for token={}", token);
		MvcResult result = mvc
				.perform(get("/survey/states/" + token))
				.andExpect(status().isOk())
				.andReturn();

		TypeReference<List<ResponseState>> t = new TypeReference<>() {
		};
		return om.readValue(result.getResponse().getContentAsString(), t);
	}

}
