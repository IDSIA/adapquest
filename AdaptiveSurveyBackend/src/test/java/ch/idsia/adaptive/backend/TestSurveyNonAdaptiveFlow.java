package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.config.WebConfig;
import ch.idsia.adaptive.backend.controller.SurveyController;
import ch.idsia.adaptive.backend.persistence.dao.AdaptiveModelRepository;
import ch.idsia.adaptive.backend.persistence.dao.QuestionRepository;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.backend.services.SessionService;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.transaction.Transactional;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		SurveyManagerService.class,
})
@Transactional
class TestSurveyNonAdaptiveFlow {

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
	AdaptiveModelRepository models;


	@BeforeEach
	void setUp() {
		// network
		BayesianNetwork bn = new BayesianNetwork();
		int A = bn.addVariable(2); // skill: A(low, high)
		int L = bn.addVariable(3); // question: low interest (a, b, c)
		int M = bn.addVariable(2); // question: medium interest (1, 2)
		int H = bn.addVariable(3); // question: high interest (*, **, ***)

		bn.addParent(L, A);
		bn.addParent(M, A);
		bn.addParent(H, A);

		BayesianFactor[] factors = new BayesianFactor[4];
		factors[A] = new BayesianFactor(bn.getDomain(A));
		factors[L] = new BayesianFactor(bn.getDomain(A, L));
		factors[M] = new BayesianFactor(bn.getDomain(A, M));
		factors[H] = new BayesianFactor(bn.getDomain(A, H));

		factors[A].setData(new double[]{.4, .6});
		factors[L].setData(new double[]{.2, .4, .7, .8, .6, .3});
		factors[M].setData(new double[]{.4, .6, .6, .4});
		factors[H].setData(new double[]{.8, .6, .3, .2, .4, .7});

		bn.setFactors(factors);

		List<String> lines = new BayesUAIWriter(bn, "").serialize();

		// single skill
		Skill skill = new Skill()
				.setName("A")
				.setLevels(List.of(
						new SkillLevel("low", 0.0),
						new SkillLevel("high", 1.0)
				));

		Map<String, Integer> map = new HashMap<>();
		map.put(skill.getName(), A);

		// create a simple model
		AdaptiveModel am = new AdaptiveModel()
				.setSkillOrder(List.of(skill.getName()))
				.setSkillToVariable(map)
				.setData(String.join("\n", lines))
				.setMixedSkillOrder(false)
				.setIsAdaptive(false);

		models.save(am);

		// question levels
		QuestionLevel low = new QuestionLevel("Low interest");
		QuestionLevel medium = new QuestionLevel("Medium interest");
		QuestionLevel high = new QuestionLevel("High interest");

		// 3 questions
		Question q1 = new Question()
				.setSkill(skill)
				.setLevel(low)
				.setAnswersAvailable(Lists.list(
						new QuestionAnswer("a"),
						new QuestionAnswer("b"),
						new QuestionAnswer("c")
				));
		Question q2 = new Question()
				.setSkill(skill)
				.setLevel(medium)
				.setAnswersAvailable(Lists.list(
						new QuestionAnswer("1"),
						new QuestionAnswer("2")
				));
		Question q3 = new Question()
				.setSkill(skill)
				.setLevel(high)
				.setAnswersAvailable(Lists.list(
						new QuestionAnswer("*"),
						new QuestionAnswer("**"),
						new QuestionAnswer("***")
				));

		questions.saveAll(List.of(q1, q2, q3));

		// create new survey
		Survey survey = new Survey()
				.setAccessCode(accessCode)
				.setDescription("This is just a description")
				.setDuration(3600L)
				.setQuestions(List.of(q1, q2, q3))
				.setModel(am);

		surveys.save(survey);

		System.out.println(survey);
	}

	@Test
	void defaultFlow() throws Exception {
		// use access code to register, init a survey, and get personal the access token
		MvcResult result;
		result = mvc.perform(get("/survey/init").param("accessCode", this.accessCode))
				.andExpect(status().isOk())
				.andReturn();

		SurveyData data = om.readValue(result.getResponse().getContentAsString(), SurveyData.class);

		Assertions.assertNotNull(data.getToken(), "Session token is null");
		Assertions.assertEquals(accessCode, data.getAccessCode(), "Access codes are different");

		// get current state of the skills
		result = mvc.perform(get("/survey/state").param("token", data.getToken()))
				.andExpect(status().isOk())
				.andReturn();

		ResponseState status = om.readValue(result.getResponse().getContentAsString(), ResponseState.class);

		// get next question
		mvc.perform(get("/survey/question")).andExpect(status().isOk());

		// post answer
		mvc.perform(post("/survey/answer")).andExpect(status().isOk());

		// get last status
		mvc.perform(get("/survey/status")).andExpect(status().isOk());
	}

	@Test
	void wrongAccessCode() {
		// TODO
		throw new NotImplementedException();
	}

	@Test
	void wrongSessionToken() {
		// TODO
		throw new NotImplementedException();
	}


}