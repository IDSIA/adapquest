package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.config.JobsConfig;
import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.config.WebConfig;
import ch.idsia.adaptive.backend.controller.ConsoleController;
import ch.idsia.adaptive.backend.controller.SurveyController;
import ch.idsia.adaptive.backend.persistence.dao.*;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.backend.services.InitializationService;
import ch.idsia.adaptive.backend.services.SessionService;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import ch.idsia.adaptive.backend.utils.TestTool;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    12.10.2021 18:22
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestApplication.class)
@WebMvcTest({
		WebConfig.class,
		PersistenceConfig.class,
		JobsConfig.class,
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
@Import(TestTool.class)
public class TestAdaptiveMultipleChoice {
	private static final Logger logger = LoggerFactory.getLogger(TestAdaptiveMultipleChoice.class);

	@Autowired
	TestTool tool;

	@Autowired
	SurveyRepository sr;

	@Autowired
	QuestionRepository qr;

	@Autowired
	AnswerRepository ar;

	final String key = "test";

	@BeforeEach
	void setUp() throws Exception {
		int i = 0;
		for (Survey survey : sr.findAll()) {
			tool.consoleSurveyRemove(key, survey.getAccessCode());
			i++;
		}
		logger.info("Setup: removed " + i + " existing surveys");
	}

	@AfterEach
	void tearDown() throws Exception {
		int i = 0;
		for (Survey survey : sr.findAll()) {
			tool.consoleSurveyRemove(key, survey.getAccessCode());
			i++;
		}
		logger.info("Teardown: removed " + i + " existing surveys");
	}

	@Test
	void getQuestionMultipleChoice() throws Exception {
		final ImportStructure structure = SurveyStructureRepository.structure3S7QMultiChoice(key);
		tool.consoleSurveyAdd(key, structure);

		final ResponseData data = tool.init(key);

		ResponseQuestion question;

		int i = 0;

		// request first question
		question = tool.next(data.token);
		logger.info("1st question: {}", question.id);
		i = answer(data, question, i);

		// request second question
		question = tool.next(data.token);
		logger.info("2nd question: {}", question.id);
		i = answer(data, question, i);

		// request third question
		question = tool.next(data.token);
		logger.info("3rd question: {}", question.id);
		i = answer(data, question, i);

		// request last question, no more available
		question = tool.next(data.token);
		logger.info("end question: {}", question);
		Assertions.assertNull(question);

		List<ResponseState> states = tool.states(data.token);

		Assertions.assertEquals(4, states.size()); // 1 initial + 3 questions

		final ResponseState state = states.get(2);
		logger.info("S0: {} S1: {} S2: {}",
				state.skillDistribution.get("S0")[0],
				state.skillDistribution.get("S1")[0],
				state.skillDistribution.get("S2")[0]
		);
	}

	private int answer(ResponseData data, ResponseQuestion question, int i) throws Exception {
		final String qid = question.name;
		switch (qid) {
			case "Q0":
				i = answerTo0(data, question, i);
				break;
			case "Q1":
				i = answerTo1(data, question, i);
				break;
			case "Q2":
				i = answerTo2(data, question, i);
				break;
		}
		return i;
	}

	private int answerTo0(ResponseData data, ResponseQuestion question, int i) throws Exception {
		// choices available: no(0), yes(1), no(2) yes(3)
		tool.answer(data.token, question.id, question.answers.get(1).id);
		final List<Answer> answers = ar.findAllBySessionTokenOrderByCreationAsc(data.token);
		Assertions.assertEquals(i + 2, answers.size());
		Assertions.assertEquals("yes", answers.get(i++).getQuestionAnswer().getText());
		Assertions.assertEquals("no", answers.get(i++).getQuestionAnswer().getText());
		return i;
	}

	private int answerTo1(ResponseData data, ResponseQuestion question, int i) throws Exception {
		List<Answer> answers;
		// choices available: no(0), yes(1), no(2) yes(3), no(4) yes(5)
		tool.answer(data.token, question.id, question.answers.get(3).id, question.answers.get(5).id);
		answers = ar.findAllBySessionTokenOrderByCreationAsc(data.token);
		Assertions.assertEquals(i + 3, answers.size());
		Assertions.assertEquals("no", answers.get(i++).getQuestionAnswer().getText());
		Assertions.assertEquals("yes", answers.get(i++).getQuestionAnswer().getText());
		Assertions.assertEquals("yes", answers.get(i++).getQuestionAnswer().getText());
		return i;
	}

	private int answerTo2(ResponseData data, ResponseQuestion question, int i) throws Exception {
		List<Answer> answers;
		// choices available: no(0), yes(1), no(2) yes(3)
		tool.answer(data.token, question.id, question.answers.get(1).id, question.answers.get(3).id);
		answers = ar.findAllBySessionTokenOrderByCreationAsc(data.token);
		Assertions.assertEquals(i + 2, answers.size());
		Assertions.assertEquals("yes", answers.get(i++).getQuestionAnswer().getText());
		Assertions.assertEquals("yes", answers.get(i++).getQuestionAnswer().getText());
		return i;
	}

	@Test
	void getQuestionMultipleChoiceDirectEvidence() throws Exception {
		final ImportStructure structure = SurveyStructureRepository.structure3S2QMultiChoiceDirect(key);

		tool.consoleSurveyAdd(key, structure);

		ResponseQuestion q;

		// survey 1 P(S | q1=0, q2=1)
		final String t1 = tool.init(key).token;
		q = tool.next(t1);
		tool.answer(t1, q.id, q.answers.get(1).id); // q1 = 1
		final ResponseState state11 = tool.state(t1);

		q = tool.next(t1);
		tool.answer(t1, q.id, q.answers.get(1).id); // q2 = 1
		final ResponseState state12 = tool.state(t1);

		// survey 2 P(S | q1=1, q2=1)
		final String t2 = tool.init(key).token;

		q = tool.next(t2);
		tool.answer(t2, q.id, q.answers.get(0).id); // q1 = 0
		final ResponseState state21 = tool.state(t2);

		q = tool.next(t2);
		tool.answer(t2, q.id, q.answers.get(1).id); // q2 = 1
		final ResponseState state22 = tool.state(t2);

		// survey 2 P(S | q1=0, q2=0)
		final String t3 = tool.init(key).token;

		q = tool.next(t3);
		tool.answer(t3, q.id, q.answers.get(0).id); // q1 = 0
		final ResponseState state31 = tool.state(t3);

		q = tool.next(t3);
		tool.answer(t3, q.id, q.answers.get(1).id); // q2 = 1
		final ResponseState state32 = tool.state(t3);

		Assertions.assertArrayEquals(new double[]{1.0, 0.0}, state11.skillDistribution.get("S2"));

		Assertions.assertArrayEquals(new double[]{0.5, 0.5}, state12.skillDistribution.get("S0"));
		Assertions.assertArrayEquals(new double[]{0.0, 1.0}, state12.skillDistribution.get("S1"));
		Assertions.assertArrayEquals(new double[]{1.0, 0.0}, state12.skillDistribution.get("S2"));

		Assertions.assertArrayEquals(new double[]{0.5, 0.5}, state21.skillDistribution.get("S2"));

		Assertions.assertArrayEquals(new double[]{1.0, 0.0}, state22.skillDistribution.get("S0"));
		Assertions.assertArrayEquals(new double[]{1.0, 0.0}, state22.skillDistribution.get("S1"));
		Assertions.assertArrayEquals(new double[]{0.0, 1.0}, state22.skillDistribution.get("S2"));

		Assertions.assertArrayEquals(new double[]{0.5, 0.5}, state31.skillDistribution.get("S2"));

		Assertions.assertArrayEquals(new double[]{1.0, 0.0}, state32.skillDistribution.get("S0"));
		Assertions.assertArrayEquals(new double[]{1.0, 0.0}, state32.skillDistribution.get("S1"));
		Assertions.assertArrayEquals(new double[]{0.0, 1.0}, state32.skillDistribution.get("S2"));
	}

}
