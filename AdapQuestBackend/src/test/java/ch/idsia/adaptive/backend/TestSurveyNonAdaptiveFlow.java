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
import ch.idsia.adaptive.backend.services.InitializationService;
import ch.idsia.adaptive.backend.services.SessionService;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import ch.idsia.adaptive.backend.utils.TestTool;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    24.11.2020 17:54
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AdapQuestBackend.class)
@WebMvcTest({
		WebConfig.class,
		PersistenceConfig.class,
		SurveyRepository.class,
		QuestionRepository.class,
		SurveyController.class,
		SessionService.class,
		SurveyManagerService.class,
		InitializationService.class,
})
@Import(TestTool.class)
@Transactional
public class TestSurveyNonAdaptiveFlow {

	@Autowired
	ObjectMapper om;

	@Autowired
	MockMvc mvc;

	@Autowired
	TestTool tool;

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
		int A = bn.addVariable(2); // skill:    A               (low, high)
		int L = bn.addVariable(3); // question: low interest    (a, b, c)
		int M = bn.addVariable(2); // question: medium interest (1, 2)
		int H = bn.addVariable(3); // question: high interest   (*, **, ***)

		bn.addParent(L, A);
		bn.addParent(M, A);
		bn.addParent(H, A);

		BayesianFactor[] factors = new BayesianFactor[4];
		factors[A] = BayesianFactorFactory.factory().domain(bn.getDomain(A)).data(new double[]{.4, .6}).get();
		factors[L] = BayesianFactorFactory.factory().domain(bn.getDomain(A, L)).data(new double[]{.2, .4, .7, .8, .6, .3}).get();
		factors[M] = BayesianFactorFactory.factory().domain(bn.getDomain(A, M)).data(new double[]{.4, .6, .6, .4}).get();
		factors[H] = BayesianFactorFactory.factory().domain(bn.getDomain(A, H)).data(new double[]{.8, .6, .3, .2, .4, .7}).get();

		bn.setFactors(factors);

		List<String> modelData = new BayesUAIWriter(bn, "").serialize();

		// single skill
		Skill skill = new Skill()
				.setName("A")
				.setVariable(A)
				.setStates(List.of(
						new SkillState("low", 0),
						new SkillState("high", 1)
				));

		// 3 questions
		q1 = new Question()
				.setQuestion("Question 1")
				.setSkill(skill)
				.setName("Low interest")
				.addAnswersAvailable(
						new QuestionAnswer("a", L, 0),
						new QuestionAnswer("b", L, 1),
						new QuestionAnswer("c", L, 2)
				);
		q2 = new Question()
				.setQuestion("Question 2")
				.setSkill(skill)
				.setName("Medium interest")
				.addAnswersAvailable(
						new QuestionAnswer("1", M, 0),
						new QuestionAnswer("2", M, 1)
				);
		q3 = new Question()
				.setQuestion("Question 3")
				.setSkill(skill)
				.setName("High interest")
				.addAnswersAvailable(
						new QuestionAnswer("*", H, 0),
						new QuestionAnswer("**", H, 1),
						new QuestionAnswer("***", H, 2)
				);

		questions.saveAll(List.of(q1, q2, q3));

		// create new survey
		Survey survey = new Survey()
				.setAccessCode(accessCode)
				.setDescription("This is just a description")
				.setDuration(3600L)
				.setQuestions(Set.of(q1, q2, q3))
				.setSkills(Set.of(skill))
				.setSkillOrder(List.of(skill.getName()))
				.setModelData(String.join("\n", modelData))
				.setMixedSkillOrder(false)
				.setIsAdaptive(false);

		surveys.save(survey);
	}

	@Test
	void defaultFlow() throws Exception {
		// use access code to register, init a survey, and get personal the access token
		ResponseData data = tool.init(accessCode);

		assertNotNull(data.token, "Session token is null");
		assertEquals(accessCode, data.code, "Access codes are different");

		// get current state of the skills
		ResponseState state1 = tool.state(data.token);

		assertNotNull(state1.skillDistribution, "Skill distribution is null!");
		assertFalse(state1.skillDistribution.isEmpty(), "Skill distribution is empty");

		// get next question
		ResponseQuestion question = tool.next(data.token);

		assertNotNull(question.question);
		assertEquals(q1.getQuestion(), question.question);
		assertEquals(3, question.answers.size());

		// post answer
		tool.answer(data.token, question.id, question.answers.get(2).id);

		// get last state
		ResponseState state2 = tool.state(data.token);

		assertEquals(1, state2.totalAnswers);
		assertEquals(1, state2.questionsPerSkill.get("A"));
		assertNotEquals(state1.skillDistribution.get("A")[0], state2.skillDistribution.get("A")[0]);
		assertNotEquals(state1.skillDistribution.get("A")[1], state2.skillDistribution.get("A")[1]);

		// check number of states
		List<ResponseState> states = tool.states(data.token);

		assertEquals(2, states.size());

		// TODO: check for values saved on database
	}

	@Test
	void wrongAccessCode() throws Exception {
		MvcResult result = mvc
				.perform(get("/survey/init/" + "Wr0ngAcc3ssC0d3!"))
				.andExpect(status().isNotFound())
				.andReturn();

		assertEquals("", result.getResponse().getContentAsString(), "Response is not empty");
	}

	@Test
	void wrongSessionToken() throws Exception {
		MvcResult result;

		// get current state of the skills
		result = mvc
				.perform(get("/survey/state/" + "this token should not exist"))
				.andExpect(status().isForbidden())
				.andReturn();

		assertEquals("", result.getResponse().getContentAsString(), "Response is not empty");
	}

}