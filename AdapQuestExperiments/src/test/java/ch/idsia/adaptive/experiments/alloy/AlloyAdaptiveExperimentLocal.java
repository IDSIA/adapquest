package ch.idsia.adaptive.experiments.alloy;

import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.QuestionAnswer;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.InitializationService;
import ch.idsia.adaptive.backend.services.commons.SurveyException;
import ch.idsia.adaptive.backend.services.commons.agents.AgentPreciseAdaptive;
import ch.idsia.adaptive.backend.services.commons.scoring.precise.ScoringFunctionExpectedEntropy;
import ch.idsia.adaptive.backend.utils.Convert;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    23.02.2021 13:35
 */
public class AlloyAdaptiveExperimentLocal {
	private static final Logger logger = LoggerFactory.getLogger(AlloyAdaptiveExperimentLocal.class);

	static final Integer THREADS = Runtime.getRuntime().availableProcessors();
	static final Integer maxQuestions = 100;
	static final Integer limitQuestionnaires = 10;

	static Map<String, AlloyModel> surveys;

	@BeforeAll
	static void setUp() throws Exception {
		surveys = new HashMap<>();

		final AlloyModel am = new AlloyModel();

		surveys.put(am.getAccessCode(), am);
	}

	List<Record> experiment(Answers ans, String accessCode) {
		final ch.idsia.adaptive.experiments.alloy.AlloyModel model = surveys.get(accessCode);
		final Survey survey = InitializationService.parseSurveyStructure(model.structure());

		long idQuestion = 0L;
		long idAnswer = 0L;

		final Set<Question> questions = survey.getQuestions();
		for (Question question : questions) {
			question.setId(idQuestion++);
			for (QuestionAnswer answer : question.getAnswersAvailable()) {
				answer.setId(idAnswer++);
			}
		}

		final AgentPreciseAdaptive as = new AgentPreciseAdaptive(survey, 0L, new ScoringFunctionExpectedEntropy());

		final List<Record> records = new ArrayList<>();

		for (int i = 0; i < maxQuestions; i++) {
			try {
				long start = System.currentTimeMillis();
				final Question nextQuestion = as.next();
				long end = System.currentTimeMillis();

				if (nextQuestion == null) {
					logger.warn("student={} no new question!", ans.id);
					break;
				}

				final String text = ans.get(nextQuestion.getQuestion());
				final QuestionAnswer qaid = nextQuestion.getAnswersAvailable().stream()
						.filter(a -> a.getText().equals(text))
						.findFirst()
						.orElse(nextQuestion.getAnswersAvailable().get(0));

				final Long aid = qaid.getId();
				final Long qid = nextQuestion.getId();

				logger.info("student={} answered questionId={} ({}) with answerId={} ({})",
						ans.id, qid, nextQuestion.getQuestion(), aid, qaid.getText());

				final Answer a = new Answer().setQuestionAnswer(qaid);
				as.check(a);

				Record r = new Record();
				r.aid = aid;
				r.qid = qid;
				r.id = ans.id;
				r.state = Convert.toResponse(as.getState());
				r.qscore = nextQuestion.getScore();
				r.qtext = nextQuestion.getQuestion();
				r.atext = qaid.getText();
				r.startTime = start;
				r.endTime = end;
				r.elapsedTime = end - start;

				records.add(r);

			} catch (SurveyException e) {
				break;
			}
		}
		logger.info("student={} completed with {} questions", ans.id, records.size());

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
		singleExperiment(alloy.getAccessCode(), index, "alloy_ans_" + index + ".tsv");
	}

	@Test
	void multipleAnsAlloy() throws Exception {
		AlloyModel alloy = new AlloyModel();
		multipleExperiment(alloy.getAccessCode(), "alloy_ans_all.tsv");
	}

	@Disabled
	@Test
	void dumpToJSON() throws Exception {
		final ObjectMapper om = new ObjectMapper();
		om.writeValue(new File("alloy/alloy.adaptive.questionnaire.json"), new AlloyModel().structure());
	}

}
