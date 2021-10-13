package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.config.WebConfig;
import ch.idsia.adaptive.backend.controller.ConsoleController;
import ch.idsia.adaptive.backend.controller.SurveyController;
import ch.idsia.adaptive.backend.persistence.dao.*;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.backend.services.InitializationService;
import ch.idsia.adaptive.backend.services.SessionService;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import ch.idsia.adaptive.backend.utils.TestTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
	private static final Logger logger = LogManager.getLogger(TestAdaptiveMultipleChoice.class);

	@Autowired
	TestTool tool;

	@Autowired
	AnswerRepository ar;

	final String key = "test";

	@Test
	void getQuestionMultipleChoice() throws Exception {
		final ImportStructure structure = SurveyStructureRepository.structure3S7QMultiChoice(key);
		tool.consoleSurveyAdd(key, structure);

		final ResponseData data = tool.init(key);

		ResponseQuestion question;
		List<Answer> answers;

		// request first question
		question = tool.next(data.token);
		logger.info("1st question: {}", question.id); // 1
		// choices available: no(0), yes(1), no(2) yes(3)
		tool.answer(data.token, question.id, question.answers.get(1).id); // NOTE: the controller expects YES answer only!
		answers = ar.findAllBySessionTokenOrderByCreationAsc(data.token);
		Assertions.assertEquals(2, answers.size());
		Assertions.assertEquals("yes", answers.get(0).getQuestionAnswer().getText());
		Assertions.assertEquals("no", answers.get(1).getQuestionAnswer().getText());

		// request second question
		question = tool.next(data.token);
		logger.info("2nd question: {}", question.id); // 3
		// choices available: no(0), yes(1), no(2) yes(3)
		tool.answer(data.token, question.id, question.answers.get(1).id, question.answers.get(3).id);
		answers = ar.findAllBySessionTokenOrderByCreationAsc(data.token);
		Assertions.assertEquals(4, answers.size());
		Assertions.assertEquals("yes", answers.get(2).getQuestionAnswer().getText());
		Assertions.assertEquals("yes", answers.get(3).getQuestionAnswer().getText());

		// request third question
		question = tool.next(data.token);
		logger.info("3rd question: {}", question.id); // 2
		// choices available: no(0), yes(1), no(2) yes(3), no(4) yes(5)
		tool.answer(data.token, question.id, question.answers.get(3).id, question.answers.get(5).id);
		answers = ar.findAllBySessionTokenOrderByCreationAsc(data.token);
		Assertions.assertEquals(7, answers.size());
		Assertions.assertEquals("no", answers.get(4).getQuestionAnswer().getText());
		Assertions.assertEquals("yes", answers.get(5).getQuestionAnswer().getText());
		Assertions.assertEquals("yes", answers.get(6).getQuestionAnswer().getText());

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

		tool.consoleSurveyRemove(key, key);
	}
}
