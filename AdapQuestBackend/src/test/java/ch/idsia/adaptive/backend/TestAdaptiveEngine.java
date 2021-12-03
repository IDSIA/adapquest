package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.config.JobsConfig;
import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.config.WebConfig;
import ch.idsia.adaptive.backend.controller.ConsoleController;
import ch.idsia.adaptive.backend.controller.SurveyController;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    02.02.2021 10:46
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestApplication.class)
@WebMvcTest(value = {
		WebConfig.class,
		JobsConfig.class,
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
public class TestAdaptiveEngine {
	private static final Logger logger = LoggerFactory.getLogger(TestAdaptiveEngine.class);

	@Autowired
	TestTool tool;

	final String code = "test";
	final String key = "test";

	@Test
	public void getQuestionWithBetterEntropy() throws Exception {
		final ImportStructure structure = SurveyStructureRepository.structure1S2Q("test");
		tool.consoleSurveyAdd(key, structure);

		ResponseData data = tool.init(code);
		ResponseQuestion question = tool.next(data.token);

		Assertions.assertEquals("Q0", question.name);

		tool.consoleSurveyRemove(key, code);
	}

	@Test
	public void getEntropyDirection() throws Exception {
		final ImportStructure structure = SurveyStructureRepository.structure2S10Q("test");
		structure.survey.scoreLowerThreshold = 0.;
		tool.consoleSurveyAdd(key, structure);

		ResponseQuestion question;
		ResponseState state;
		ResponseData data = tool.init(code);

		List<String> sequence = new ArrayList<>();
		List<ResponseState> rqsList = new ArrayList<>();

		logger.info("\n");
		state = tool.state(data.token);
		logger.info("Entropy S0: {}\t{}", state.scoreDistribution.get("S0"), state.skillDistribution.get("S0"));
		logger.info("Entropy S1: {}\t{}\n", state.scoreDistribution.get("S1"), state.skillDistribution.get("S1"));

		for (int i = 0; i < 10; i++) {
			question = tool.next(data.token);
			if (question == null)
				break;

			tool.answer(data.token, question.id, question.answers.get(1).id);
			state = tool.state(data.token);
			sequence.add(question.name);
			logger.info("Question:   {}", question.id);
			logger.info("Entropy S0: {}\t{}", state.scoreDistribution.get("S0"), state.skillDistribution.get("S0"));
			logger.info("Entropy S1: {}\t{}\n", state.scoreDistribution.get("S1"), state.skillDistribution.get("S1"));

			rqsList.add(state);
		}

		question = tool.next(data.token);
		Assertions.assertNull(question);

		tool.consoleSurveyRemove(key, code);

		logger.info("Sequence:   {}\n", String.join(" ", sequence));

		rqsList.forEach(s0 -> logger.info("Entropy S0: {}\t{}", s0.scoreDistribution.get("S0"), s0.skillDistribution.get("S0")));
		logger.info("\n");
		rqsList.forEach(s0 -> logger.info("Entropy S1: {}\t{}", s0.scoreDistribution.get("S1"), s0.skillDistribution.get("S1")));
		logger.info("\n");
	}

	@Test
	public void numberOfQuestionsWithDifferentEntropyThresholds() throws Exception {
		final Random r = new Random(42);
		final int[] rs = IntStream.range(0, 20).map(x -> r.nextInt(2)).toArray();

		final double[] thresholds = {.5, .2, .1, .05, .0};
		final ImportStructure structure = SurveyStructureRepository.structure1S20Q("test");

		final List<ResponseState> states = new ArrayList<>();
		final List<Integer> questionsDone = new ArrayList<>();
		final List<String> sequences = new ArrayList<>();

		ResponseQuestion question;
		ResponseData data;

		for (double th : thresholds) {
			logger.info("Threshold: {}", th);

			structure.survey.scoreLowerThreshold = th;
			tool.consoleSurveyAdd(key, structure);

			data = tool.init(code);

			List<String> sequence = new ArrayList<>();
			Integer i = 0;

			while ((question = tool.next(data.token)) != null) {
				int x = Integer.parseInt(question.name.substring(1)) - 1;
				tool.answer(data.token, question.id, question.answers.get(rs[x]).id);
				sequence.add(question.name);
				i++;
			}

			states.add(tool.state(data.token));
			sequences.add(String.join(" ", sequence));
			questionsDone.add(i);

			tool.consoleSurveyRemove(key, "test");
			logger.info("\n");
		}

		for (int i = 0; i < thresholds.length; i++) {
			logger.info("Threshold:      {}", thresholds[i]);
			logger.info("Questions done: {}", questionsDone.get(i));
			logger.info("Sequence:       {}", sequences.get(i));
			logger.info("Entropy:        {}", states.get(i).scoreDistribution.get("S0"));
			logger.info("Distribution:   {}", states.get(i).skillDistribution.get("S0"));
			logger.info("\n");
		}
	}

	@Test
	public void numberOfQuestionsWithDifferentMinQuestions() throws Exception {
		final ImportStructure structure = SurveyStructureRepository.structure1S20Q("test");

		ResponseQuestion question;
		ResponseData data;

		structure.survey.scoreLowerThreshold = .1;
		structure.survey.questionPerSkillMin = 3;
		tool.consoleSurveyAdd(key, structure);

		data = tool.init(code);

		int i = 0;
		while ((question = tool.next(data.token)) != null) {
			tool.answer(data.token, question.id, question.answers.get(0).id);
			i++;
		}

		tool.consoleSurveyRemove(key, "test");

		Assertions.assertTrue(i > 3);

		structure.survey.questionPerSkillMin = 5;
		tool.consoleSurveyAdd(key, structure);

		data = tool.init(code);

		i = 0;
		while ((question = tool.next(data.token)) != null) {
			tool.answer(data.token, question.id, question.answers.get(0).id);
			i++;
		}

		Assertions.assertTrue(i > 5);
		tool.consoleSurveyRemove(key, code);
	}
}