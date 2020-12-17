package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.config.WebConfig;
import ch.idsia.adaptive.backend.controller.SurveyController;
import ch.idsia.adaptive.backend.persistence.dao.QuestionRepository;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.backend.services.SessionService;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.transaction.Transactional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
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

	@Autowired
	ObjectMapper om;

	@Autowired
	MockMvc mvc;

	@Autowired
	SurveyRepository surveys;

	@Autowired
	QuestionRepository questions;

	String accessCode = "Code123";
	Question q1, q2, q3;

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

//		BeliefPropagation<BayesianFactor> inf = new BeliefPropagation<>(bn);
//
//		TIntIntHashMap obs = new TIntIntHashMap();
//
//		obs.put(L, 0);
//		inf.setEvidence(obs);
//		System.out.println(inf.query(A));
//
//		obs.put(L, 1);
//		inf.setEvidence(obs);
//		System.out.println(inf.query(A));
//
//		obs.put(L, 2);
//		inf.setEvidence(obs);
//		System.out.println(inf.query(A));

		List<String> modelData = new BayesUAIWriter(bn, "").serialize();

		// single skill
		Skill skill = new Skill()
				.setName("A")
				.setVariable(A)
				.setLevels(List.of(
						new SkillLevel("low", 0.0),
						new SkillLevel("high", 1.0)
				));

		// question levels
		QuestionLevel low = new QuestionLevel().setName("Low interest").setVariable(L);
		QuestionLevel medium = new QuestionLevel().setName("Medium interest").setVariable(M);
		QuestionLevel high = new QuestionLevel().setName("High interest").setVariable(H);

		// 3 questions
		q1 = new Question()
				.setQuestion("Question 1")
				.setSkill(skill)
				.setLevel(low)
				.addAnswersAvailable(
						new QuestionAnswer().setText("a").setState(0),
						new QuestionAnswer().setText("b").setState(1),
						new QuestionAnswer().setText("c").setState(2)
				);
		q2 = new Question()
				.setQuestion("Question 2")
				.setSkill(skill)
				.setLevel(medium)
				.addAnswersAvailable(
						new QuestionAnswer().setText("1").setState(0),
						new QuestionAnswer().setText("2").setState(1)
				);
		q3 = new Question()
				.setQuestion("Question 3")
				.setSkill(skill)
				.setLevel(high)
				.addAnswersAvailable(
						new QuestionAnswer().setText("*").setState(0),
						new QuestionAnswer().setText("**").setState(1),
						new QuestionAnswer().setText("***").setState(2)
				);

		questions.saveAll(List.of(q1, q2, q3));

		// create new survey
		Survey survey = new Survey()
				.setAccessCode(accessCode)
				.setDescription("This is just a description")
				.setDuration(3600L)
				.setQuestions(List.of(q1, q2, q3))
				.setSkills(List.of(skill))
				.setSkillOrder(List.of(skill.getName()))
				.setModelData(String.join("\n", modelData))
				.setMixedSkillOrder(false)
				.setIsAdaptive(false);

		surveys.save(survey);

		System.out.println(survey);
	}

	@Test
	void defaultFlow() throws Exception {
		// use access code to register, init a survey, and get personal the access token
		MvcResult result;
		result = mvc
				.perform(get("/survey/init")
						.param("accessCode", this.accessCode)
				)
				.andExpect(status().isOk())
				.andReturn();

		ResponseData data = om.readValue(result.getResponse().getContentAsString(), ResponseData.class);

		assertNotNull(data.token, "Session token is null");
		assertEquals(accessCode, data.code, "Access codes are different");

		// get current state of the skills
		result = mvc
				.perform(get("/survey/state")
						.param("token", data.token)
				)
				.andExpect(status().isOk())
				.andReturn();

		ResponseState status1 = om.readValue(result.getResponse().getContentAsString(), ResponseState.class);

		assertNotNull(status1.skillDistribution, "Skill distribution is null!");
		assertFalse(status1.skillDistribution.isEmpty(), "Skill distribution is empty");

		// get next question
		result = mvc
				.perform(get("/survey/question")
						.param("token", data.token)
				)
				.andExpect(status().isOk())
				.andReturn();

		ResponseQuestion question = om.readValue(result.getResponse().getContentAsString(), ResponseQuestion.class);

		assertNotNull(question.question);
		assertEquals(q1.getQuestion(), question.question);
		assertEquals(3, question.answers.size());

		// post answer
		mvc
				.perform(post("/survey/answer")
						.param("token", data.token)
						.param("question", "" + question.id)
						.param("answer", "" + question.answers.get(2).id)
				).andExpect(status().isOk());

		// get last status1
		result = mvc
				.perform(get("/survey/state")
						.param("token", data.token)
				)
				.andExpect(status().isOk())
				.andReturn();

		ResponseState status2 = om.readValue(result.getResponse().getContentAsString(), ResponseState.class);

		assertEquals(1, status2.totalAnswers);
		assertEquals(1, status2.questionsPerSkill.get("A"));
		assertNotEquals(status1.skillDistribution.get("A")[0], status2.skillDistribution.get("A")[0]);
		assertNotEquals(status1.skillDistribution.get("A")[1], status2.skillDistribution.get("A")[1]);

		// TODO: check for values saved on database
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