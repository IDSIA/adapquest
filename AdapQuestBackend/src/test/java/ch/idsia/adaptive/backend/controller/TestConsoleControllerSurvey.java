package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.SurveyStructureRepository;
import ch.idsia.adaptive.backend.TestApplication;
import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.config.WebConfig;
import ch.idsia.adaptive.backend.persistence.dao.*;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.backend.services.InitializationService;
import ch.idsia.adaptive.backend.services.SessionService;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import ch.idsia.adaptive.backend.utils.TestTool;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.transaction.Transactional;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    29.01.2021 19:46
 */
@Disabled
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
@Transactional
public class TestConsoleControllerSurvey {

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

	@Autowired
	TestTool tool;

	final String code = "test";
	final String key = "test";

	@BeforeEach
	void setUp() throws Exception {
		final ImportStructure structure = SurveyStructureRepository.structure2S2Q(code);
		tool.consoleSurveyAdd(key, structure);

		ResponseData data = tool.init(code);
		ResponseQuestion question = tool.next(data.token);

		tool.answer(data.token, question.id, question.answers.get(0).id);
	}

	@Test
	public void getAllStatesForSurvey() throws Exception {
		List<ResponseState> rs = tool.consoleStates(key, code);

		Assertions.assertEquals(1, sessions.count());
		Assertions.assertEquals(states.count(), rs.size());

		tool.consoleSurveyRemove(key, code);
	}

	@Test
	public void removeSurvey() throws Exception {
		tool.consoleSurveyRemove(key, code);

		Assertions.assertEquals(0, sessions.count());
		Assertions.assertEquals(0, states.count());
		Assertions.assertEquals(0, questions.count());
		Assertions.assertEquals(0, answers.count());
		Assertions.assertEquals(0, questionAnswers.count());
		Assertions.assertEquals(0, surveys.count());
	}

	@Test
	public void cleanSurvey() throws Exception {
		tool.consoleSurveyClean(key, code);

		Assertions.assertEquals(0, sessions.count());
		Assertions.assertEquals(0, states.count());
		Assertions.assertEquals(0, answers.count());
		Assertions.assertEquals(4, questionAnswers.count());
		Assertions.assertEquals(2, questions.count());
		Assertions.assertEquals(1, surveys.count());

		tool.consoleSurveyRemove(key, code);
	}

}