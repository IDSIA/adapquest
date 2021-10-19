package ch.idsia.adaptive.experiments.language;

import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseSkill;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.experiments.Tool;
import ch.idsia.adaptive.experiments.ToolLocalhost;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    28.01.2021 16:02
 */
public class LanguageAdaptiveExperiment {
	private static final Logger logger = LoggerFactory.getLogger(LanguageAdaptiveExperiment.class);

	static final Integer CORES = Runtime.getRuntime().availableProcessors() * 2;

	public Integer FIRST_STUDENT = 0; // inclusive, 0-based
	public Integer LAST_STUDENT = 10; // exclusive

	Tool tool;
	LanguageTest survey;

	List<Student> students;

	/**
	 * There we register a new key for the experiment and, if needed, we create a new remote survey to experiment with.
	 *
	 * @throws Exception if something goes very bad...
	 */
	@BeforeEach
	void setUp() throws Exception {
		survey = new LanguageTest();

		tool = new ToolLocalhost();
		tool.newApiKey("");

		// read all students data
		students = getStudents();
		logger.info("found {} students", students.size());

		if (!tool.checkSurvey(LanguageTest.accessCode)) {
			/*
				if we don't already have the survey in the remote application, create it
				this is useful when we need to restart multiple times because of errors...
			*/
			tool.addSurvey(survey.structure());
			logger.info("Survey with accessCode={} created", LanguageTest.accessCode);
		}
	}

	/**
	 * There we remove the survey and the key we added in the remote application.
	 *
	 * @throws Exception if something goes very bad...
	 */
	@AfterEach
	void tearDown() throws Exception {
		tool.removeSurvey(LanguageTest.accessCode);
		tool.removeKey();
	}

	/**
	 * Utility method to read all the data on the answers from the students.
	 *
	 * @return a list of answers for each students
	 * @throws IOException if the file languageTestDeAnswers.csv is not found
	 */
	private List<Student> getStudents() throws IOException {
		logger.info("Reading answer file.");
		try (BufferedReader br = new BufferedReader(new InputStreamReader(LanguageAdaptiveExperiment.class.getResourceAsStream("/languageTestDeAnswers.csv")))) {
			final List<String[]> answers = br.lines().map(x -> x.split(",")).collect(Collectors.toList());

			final String[] header = answers.get(0);
			final List<Student> list = IntStream.range(1, answers.size())
					.mapToObj(answers::get)
					.map(a -> new Student(header, a))
					.collect(Collectors.toList());
			for (int i = 0; i < list.size(); i++) {
				list.get(i).i = i;
			}
			return list;
		}
	}

	/**
	 * Just testing if the {@link #setUp()} and {@link #tearDown()} methods work.
	 */
	@Test
	void dummyForSetUpAndTearDown() {
		logger.info("Dummy");
	}

	/**
	 * There we experiments a single student. The student is the one set by the {@link #FIRST_STUDENT} parameter of this
	 * test case.
	 * <p>
	 * This test will write a file named "student.#.txt" where some results (such as total answers, questions per skill)
	 * state distribution, and entropy per skill) are stored.
	 *
	 * @throws Exception if something is not right...
	 */
	@Test
	void adaptiveOneStudent() throws Exception {
		logger.info("Single students with id={}", FIRST_STUDENT);

		final Student student = students.get(FIRST_STUDENT);
		final String token = tool.init(LanguageTest.accessCode);

		student.token = token;

		ResponseQuestion nextQuestion;

		// we perform questions until the survey is finished
		while ((nextQuestion = tool.nextQuestion(token)) != null) {
			// this is an answer to this question
			final Integer answer = student.get(nextQuestion.name);
			final Long aid = nextQuestion.answers.get(answer).id;
			final Long qid = nextQuestion.id;

			logger.info("token={} student={} answered questionId={} with answerId={} ({})", token, FIRST_STUDENT, qid, aid, answer);
			tool.answer(token, qid, aid);
		}

		// query for the last state
		ResponseState state = tool.state(token);

		// saving everything to file
		StringBuilder sb = new StringBuilder();
		sb.append("Total answers:").append(state.totalAnswers).append("\n");
		for (ResponseSkill skill : state.skills) {
			final String s = skill.name;
			sb.append("Skill: ").append(s).append("\n")
					.append("\tQuestions: ").append(state.questionsPerSkill.get(s)).append("\n")
					.append("\tLevel:     ").append(skill.states.get(argmax(state.skillDistribution.get(s))).name).append("\n")
					.append("\tEntropy:   ").append(state.scoreDistribution.get(s)).append("\n")
			;
		}
		Files.write(Paths.get("student." + FIRST_STUDENT + ".txt"), sb.toString().getBytes());

		logger.info("Saved to {} as number {}", Paths.get("").toAbsolutePath(), FIRST_STUDENT);
	}

	/**
	 * Same as {@link #adaptiveOneStudent()} but with multiple students in parallel. The number of thread is indicated
	 * by the {@link #CORES} field in this test class. This will perform a survey for all the students between
	 * {@link #FIRST_STUDENT} (inclusive) and {@link #LAST_STUDENT} (exclusive).
	 * <p>
	 * At the end, a file named "adaptiveLanguageTest.results.#1.#2.tsv" is written, where #1 is {@link #FIRST_STUDENT}
	 * and #2 is {@link #LAST_STUDENT}. This file collect the level achieved for each student in each skill.
	 *
	 * @throws Exception if something goes wrong
	 */
	@Test
	public void adaptiveMultipleStudent() throws Exception {
		logger.info("Multiple students from {} to {} over {} core(s)", FIRST_STUDENT, LAST_STUDENT, CORES);

		// we are using Java's concurrent framework
		ExecutorService es = Executors.newFixedThreadPool(CORES);

		final List<Callable<Void>> tasks = IntStream.range(Math.max(FIRST_STUDENT, 0), Math.min(LAST_STUDENT, students.size()))
				.mapToObj(students::get)
				.map(student -> (Callable<Void>) () -> {
					// each task will have it's personal Tool object. No key is required to perform a survey
					final Tool tool = new ToolLocalhost();
					final String token = tool.init(LanguageTest.accessCode);

					student.token = token;

					ResponseQuestion nextQuestion;
					while ((nextQuestion = tool.nextQuestion(token)) != null) {
						final Integer answer = student.get(nextQuestion.name);
						final Long aid = nextQuestion.answers.get(answer).id;
						final Long qid = nextQuestion.id;

						logger.info("token={} student={} answered questionId={} with answerId={} ({})", token, student.i, qid, aid, answer);
						tool.answer(token, qid,
								aid // 0 is wrong 1 is correct
						);

						// save all states inside student to follow its progresses
						student.states.add(tool.state(token));
					}
					return null;
				})
				.collect(Collectors.toList());

		logger.info("Collected {} task(s)", tasks.size());

		es.invokeAll(tasks); // submit all tasks
		es.shutdown(); // wait until the end, then shutdown and procede with the code

		// collect the results
		List<String> skills = new ArrayList<>();
		List<String> lines = new ArrayList<>();
		students.forEach(student -> student.states.forEach(state -> {
			// header
			if (lines.isEmpty()) {
				skills.addAll(state.skills.stream().map(x -> x.name).collect(Collectors.toList()));

				final List<String> header = new ArrayList<>();
				header.add("index");
				header.add("creationTime");
				header.add("totalAnswers");
				header.add("last");
				skills.forEach(skill -> {
					header.add(skill + "Distribution");
					header.add(skill + "Entropy");
					header.add(skill + "Questions");
				});

				lines.add(String.join("\t", header));
			}

			// content
			List<String> line = new ArrayList<>();
			line.add("" + student.i);
			line.add("" + state.creationTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli());
			line.add("" + state.totalAnswers);
			line.add("" + state.creationTime.equals(student.last().creationTime));
			skills.forEach(skill -> {
				line.add(Arrays.toString(state.skillDistribution.get(skill)));
				line.add("" + state.scoreDistribution.get(skill));
				line.add("" + state.questionsPerSkill.get(skill));
			});

			lines.add(String.join("\t", line));
		}));

		Files.write(Paths.get("adaptiveLanguageTest.results." + FIRST_STUDENT + "." + LAST_STUDENT + ".tsv"), lines);
	}

	/**
	 * Utility method to find the index of the max value in an array of double.
	 * This is implementation #154845468464131 of such method in Java.
	 *
	 * @param doubles array to search in
	 * @return the index where we have the maximum value in the array
	 */
	private static int argmax(double[] doubles) {
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

}
