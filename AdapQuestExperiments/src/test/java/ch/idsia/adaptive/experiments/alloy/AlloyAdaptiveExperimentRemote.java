package ch.idsia.adaptive.experiments.alloy;

import ch.idsia.adaptive.backend.persistence.responses.ResponseAnswer;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.experiments.Tool;
import ch.idsia.adaptive.experiments.ToolLocalhost;
import ch.idsia.adaptive.experiments.models.AbstractAdaptiveModel;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    23.02.2021 13:35
 */
public class AlloyAdaptiveExperimentRemote {
	private static final Logger logger = LoggerFactory.getLogger(AlloyAdaptiveExperimentRemote.class);

	static final Integer THREADS = Runtime.getRuntime().availableProcessors() * 2;
	static final Integer maxQuestions = 100;
	static final Integer limitQuestionnaires = 10;

	static final Supplier<Tool> toolBuilder = ToolLocalhost::new;

	static Tool ctrl;

	@BeforeAll
	static void setUp() throws Exception {
		ctrl = toolBuilder.get();

		// test for existing key
		final Path apiKeyPath = Paths.get(".apikey");
		if (Files.exists(apiKeyPath)) {
			final String key = Files.readString(apiKeyPath);
			ctrl.setKey(key);
		}

		// test if current key is valid...
		if (!ctrl.isKeyValid()) {
			// ...if not request a new key
			ctrl.newApiKey("");
		}
	}

	@AfterAll
	static void tearDown() throws Exception {
		ctrl.removeAllSurvey();
		ctrl.removeKey();
	}

	void checkSurvey(AbstractAdaptiveModel survey) throws Exception {
		final String accessCode = survey.getAccessCode();
		if (!ctrl.checkSurvey(accessCode)) {
			/*
				if we don't already have the survey in the remote application, create it
				this is useful when we need to restart multiple times because of errors...
			 */
			ctrl.addSurvey(survey.structure());
			logger.info("Survey with accessCode={} created", accessCode);
		}
	}

	List<Record> experiment(Answers ans, String accessCode) throws Exception {
		final Tool tool = toolBuilder.get();
		final List<Record> records = new ArrayList<>();
		final String token = tool.init(accessCode);

		for (int i = 0; i < maxQuestions; i++) {
			final ResponseQuestion nextQuestion = tool.nextQuestion(token);
			if (nextQuestion == null)
				break;

			final String text = ans.get(nextQuestion.question);
			final ResponseAnswer raid = nextQuestion.answers.stream()
					.filter(a -> a.text.equals(text))
					.findFirst()
					.orElse(nextQuestion.answers.get(0));

			final Long aid = raid.id;
			final Long qid = nextQuestion.id;

			logger.info("token={} student={} answered questionId={} ({}) with answerId={} ({})",
					token, ans.id, qid, nextQuestion.question, aid, raid.text);
			tool.answer(token, qid, aid);

			Record r = new Record();
			r.aid = aid;
			r.qid = qid;
			r.id = ans.id;
			r.state = tool.state(token);
			r.qscore = nextQuestion.score;
			r.qtext = nextQuestion.question;
			r.atext = raid.text;

			records.add(r);
		}

		return records;
	}

	void singleExperiment(String accessCode, int index, String csvFilename) throws Exception {
		// first set of answers
		final Answers ans = Answers.get().get(index);
		final List<Record> records = experiment(ans, accessCode);

		Files.write(Paths.get(csvFilename), Record.toCSV(records));
	}

	void multipleExperiment(String accessCode, String csvFilename) throws Exception {
		final List<Answers> anss = Answers.get();
		Collections.shuffle(anss, new Random(0));

		final ExecutorService es = Executors.newFixedThreadPool(THREADS);

		final List<Callable<List<String>>> tasks = anss.stream()
				.filter(x -> x.size() > 10)
				.limit(limitQuestionnaires)
				.map(ans -> (Callable<List<String>>) () -> {
					// each task will have it's personal Tool object. No key is required to perform a survey
					final List<Record> records = experiment(ans, accessCode);
					return Record.toCSV(records);
				})
				.collect(Collectors.toList());

		logger.info("Collected {} task(s)", tasks.size());

		final List<Future<List<String>>> futures = es.invokeAll(tasks);
		es.shutdown();

		// collecting results
		List<String> lines = new ArrayList<>();

		for (Future<List<String>> future : futures) {
			final List<String> l = future.get();

			if (lines.isEmpty()) {
				lines.add(l.get(0));
			}
			lines.addAll(l.subList(1, l.size()));
		}

		Files.write(Paths.get(csvFilename), lines);
	}

	@Test
	void singleAnsAlloy() throws Exception {
		int index = 0;
		AlloyModel alloy = new AlloyModel();
		checkSurvey(alloy);
		singleExperiment(alloy.getAccessCode(), index, "alloy_ans_" + index + ".tsv");
	}

	@Test
	void multipleAnsAlloy5() throws Exception {
		AlloyModel alloy = new AlloyModel();
		checkSurvey(alloy);
		multipleExperiment(alloy.getAccessCode(), "alloy_ans_all.tsv");
	}

}
