package ch.idsia.adaptive.experiments;

import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseSkill;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.experiments.models.LanguageTestGerman;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    28.01.2021 16:02
 */
public class ExperimentLanguageAdaptiveTest {
	private static final Logger logger = LogManager.getLogger(ExperimentLanguageAdaptiveTest.class);

	static final Integer CORES = Runtime.getRuntime().availableProcessors();

	public Integer FIRST_STUDENT = 0; // inclusive, 0-based
	public Integer LAST_STUDENT = 10; // exclusive

	static final String host = "artemis.idsia.ch";
	static final Integer port = 8080;
	static final String MAGIC = "QWRhcHRpdmUgU3VydmV5";

	Tool tool;
	LanguageTestGerman survey;

	List<Student> students;

	@BeforeEach
	void setUp() throws Exception {
		survey = new LanguageTestGerman();

		tool = new Tool(host, port);
		tool.newApiKey(MAGIC);

		logger.info("Key: {}", tool.getKey());
		Files.write(Paths.get(".apikey"), tool.getKey().getBytes());

		students = getStudents();
		logger.info("found {} students", students.size());

		try {
			tool.init(LanguageTestGerman.accessCode);
			logger.info("Survey with accessCode={} available", LanguageTestGerman.accessCode);
		} catch (Exception e) {
			logger.info("Survey with accessCode={} NOT available", LanguageTestGerman.accessCode);
			tool.addSurvey(survey.structure());
			tool.init(LanguageTestGerman.accessCode);
			logger.info("Survey with accessCode={} created", LanguageTestGerman.accessCode);
		}
	}

	@AfterEach
	void tearDown() throws Exception {
		tool.deleteCurrentKey();
	}

	private List<Student> getStudents() throws IOException {
		logger.info("Reading answer file.");
		try (BufferedReader br = new BufferedReader(new InputStreamReader(ExperimentLanguageAdaptiveTest.class.getResourceAsStream("/languageTestAnswers.csv")))) {
			final List<String[]> answers = br.lines().map(x -> x.split(",")).collect(Collectors.toList());

			final String[] header = answers.get(0);
			return IntStream.range(1, answers.size())
					.mapToObj(answers::get)
					.map(a -> new Student(header, a))
					.collect(Collectors.toList());
		}
	}

	@Test
	void adaptiveOneStudent() throws Exception {
		logger.info("Single students with id={}", FIRST_STUDENT);

		final Student student = students.get(FIRST_STUDENT);
		final String token = tool.init(LanguageTestGerman.accessCode);

		ResponseQuestion nextQuestion;

		while ((nextQuestion = tool.nextQuestion(token)) != null) {
			tool.answer(token,
					nextQuestion.id, // this is an answer to this question
					nextQuestion.answers.get(student.get(nextQuestion.explanation)).id // 0 is always correct 1 is always wrong
			);
		}

		ResponseState state = tool.state(token);

		StringBuilder sb = new StringBuilder();
		sb.append("Total answers:").append(state.totalAnswers).append("\n");
		for (ResponseSkill skill : state.skills) {
			final String s = skill.name;
			sb.append("Skill: ").append(s).append("\n")
					.append("\tQuestions: ").append(state.questionsPerSkill.get(s)).append("\n")
					.append("\tLevel:     ").append(skill.states.get(argmax(state.skillDistribution.get(s))).name).append("\n")
					.append("\tEntropy:   ").append(state.entropyDistribution.get(s)).append("\n")
			;
		}
		Files.write(Paths.get("student." + FIRST_STUDENT + ".txt"), sb.toString().getBytes());

		logger.info("Saved to {} as number {}", Paths.get("").toAbsolutePath(), FIRST_STUDENT);
	}

	@Test
	public void testAdaptiveMultipleStudent() throws Exception {
		logger.info("Multiple students from {} to {} over {} core(s)", FIRST_STUDENT, LAST_STUDENT, CORES);

		ExecutorService es = Executors.newFixedThreadPool(CORES);

		final List<Callable<Void>> tasks = IntStream.range(Math.max(FIRST_STUDENT, 0), Math.min(LAST_STUDENT, students.size()))
				.mapToObj(students::get)
				.map(student -> (Callable<Void>) () -> {
					final Tool tool = new Tool(host, port);
					final String token = tool.init(LanguageTestGerman.accessCode);

					ResponseQuestion nextQuestion;
					while ((nextQuestion = tool.nextQuestion(token)) != null) {
						tool.answer(token,
								nextQuestion.id, // this is an answer to this question
								nextQuestion.answers.get(student.get(nextQuestion.explanation)).id // 0 is always correct 1 is always wrong
						);
					}

					student.state = tool.state(token);
					return null;
				})
				.collect(Collectors.toList());

		logger.info("Collected {} task(s)", tasks.size());

		es.invokeAll(tasks);
		es.shutdown();

		List<String> lines = new ArrayList<>();
		students.forEach(student -> {
			final ResponseState state = student.state;
			if (state == null)
				return;

			if (lines.isEmpty()) {
				final List<String> header = state.skills.stream().map(x -> x.name).collect(Collectors.toList());
				lines.add(String.join("\t", header));
			}

			final List<String> line = state.skills.stream()
					.map(skill -> skill.states.get(argmax(state.skillDistribution.get(skill.name))).name)
					.collect(Collectors.toList());
			lines.add(String.join("\t", line));
		});

		Files.write(Paths.get("adaptiveLanguageTest.results." + FIRST_STUDENT + "." + LAST_STUDENT + ".tsv"), lines);
	}

	private int argmax(double[] doubles) {
		int v = 0;
		double d = doubles[v];

		for (int i = 1; i < doubles.length; i++) {
			if (doubles[i] > d) {
				d = doubles[i];
				v = i;
			}
		}
		return v;
	}

	/**
	 * Dummy class to identify all the answers of a student.
	 */
	static class Student extends HashMap<String, Integer> {
		ResponseState state;

		public Student(String[] header, String[] answers) {
			IntStream.range(0, header.length)
					.forEach(i -> put(header[i], Integer.parseInt(answers[i].trim())));
		}
	}
}
