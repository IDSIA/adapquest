package ch.idsia.adaptive.backend.utils;

import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    02.02.2021 07:55
 */
@TestComponent
public class TestTool {

	ObjectMapper om;
	MockMvc mvc;

	@Autowired
	public TestTool(ObjectMapper om, MockMvc mvc) {
		this.om = om;
		this.mvc = mvc;
	}

	public ResponseData init(String accessCode) throws Exception {
		MvcResult result;
		result = mvc
				.perform(get("/survey/init/" + accessCode))
				.andExpect(status().isOk())
				.andReturn();

		return om.readValue(result.getResponse().getContentAsString(), ResponseData.class);
	}

	public ResponseQuestion next(String token) throws Exception {
		MvcResult result = mvc
				.perform(get("/survey/question/" + token))
				.andExpect(status().isOk())
				.andReturn();

		return om.readValue(result.getResponse().getContentAsString(), ResponseQuestion.class);
	}

	public void answer(String token, Long questionId, Long answerId) throws Exception {
		mvc
				.perform(post("/survey/answer/" + token)
						.contentType(MediaType.APPLICATION_FORM_URLENCODED)
						.param("question", "" + questionId)
						.param("answer", "" + answerId)
				).andExpect(status().isOk());
	}

	public ResponseState state(String token) throws Exception {
		MvcResult result = mvc
				.perform(get("/survey/state/" + token))
				.andExpect(status().isOk())
				.andReturn();

		return om.readValue(result.getResponse().getContentAsString(), ResponseState.class);
	}

	public List<ResponseState> states(String token) throws Exception {
		MvcResult result = mvc
				.perform(get("/survey/states/" + token))
				.andExpect(status().isOk())
				.andReturn();

		TypeReference<List<ResponseState>> t = new TypeReference<>() {
		};
		return om.readValue(result.getResponse().getContentAsString(), t);
	}
}
