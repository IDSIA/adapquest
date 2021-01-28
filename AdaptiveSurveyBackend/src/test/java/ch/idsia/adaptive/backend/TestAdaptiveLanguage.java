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
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Ignore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.transaction.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    26.01.2021 13:42
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = AdaptiveSurveyBackend.class)
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
@Transactional
public class TestAdaptiveLanguage {

	public static final Double ENTROPY_STOP_THRESHOLD_MAX = .0;

	private static final String ACCESS_CODE = "LanguageTestGerman";
	public static final Double ENTROPY_STOP_THRESHOLD_MIN = .25;
	public static final Integer FIRST_STUDENT = 0; // inclusive, 0-based
	public static final Integer LAST_STUDENT = 10; // exclusive
	public static final Integer CORES = Runtime.getRuntime().availableProcessors();
	private static final Logger logger = LogManager.getLogger(TestAdaptiveLanguage.class);
	@Autowired
	ObjectMapper om;

	@Autowired
	MockMvc mvc;

	@Autowired
	SurveyRepository surveys;

	@Autowired
	QuestionRepository questions;

	List<Student> students;

	private Skill addSurveySkill(int variable, String name) {
		logger.info("Adding skill {}", name);
		return new Skill().setName(name)
				.setVariable(variable)
				.setStates(List.of(
						new SkillLevel("A1", 0),
						new SkillLevel("A2", 1),
						new SkillLevel("A3", 2),
						new SkillLevel("A4", 3)
				));
	}

	private Q addQuestion(BayesianNetwork bn, int idx, int skill, int difficulty, double[][] data) {
		logger.info("Adding to network question node={} difficulty={} for skill={}", idx, difficulty, skill);
		int q = bn.addVariable(2);
		bn.addParent(q, skill);
		bn.setFactor(q, new BayesianFactor(bn.getDomain(skill, q), data[difficulty]));
		return new Q(q, skill, difficulty, "Q" + idx);
	}

	private List<Student> getStudents() throws IOException {
		logger.info("Reading answer file.");
		try (BufferedReader br = new BufferedReader(new InputStreamReader(TestAdaptiveLanguage.class.getResourceAsStream("/languageTestAnswers.csv")))) {
			final List<String[]> answers = br.lines().map(x -> x.split(",")).collect(Collectors.toList());

			final String[] header = answers.get(0);
			return IntStream.range(1, answers.size())
					.mapToObj(answers::get)
					.map(a -> new Student(header, a))
					.collect(Collectors.toList());
		}
	}

	private String init() throws Exception {
		logger.info("initialization new survey");
		MvcResult result;
		result = mvc
				.perform(get("/survey/init")
						.param("accessCode", ACCESS_CODE)
				)
				.andExpect(status().isOk())
				.andReturn();

		ResponseData data = om.readValue(result.getResponse().getContentAsString(), ResponseData.class);
		logger.info("Survey initialized with token={}", data.token);

		return data.token;
	}

	private ResponseQuestion getNextQuestion(String token) throws Exception {
		// get next question
		MvcResult result = mvc
				.perform(get("/survey/question")
						.param("token", token)
				)
				.andExpect(status().is2xxSuccessful())
				.andReturn();

		if (result.getResponse().getStatus() == HttpStatus.NO_CONTENT.value())
			return null;

		final ResponseQuestion rq = om.readValue(result.getResponse().getContentAsString(), ResponseQuestion.class);

		logger.info("new question for token={}: id={} difficulty={}", token, rq.id, rq.explanation);

		return rq;
	}

	private void postAnswer(String token, Long questionId, Long answerId) throws Exception {
		logger.info("answering with token={} questionId={} answerId={}", token, questionId, answerId);
		mvc
				.perform(post("/survey/answer")
						.param("token", token)
						.param("question", "" + questionId)
						.param("answer", "" + answerId)
				).andExpect(status().isOk());
	}

	private ResponseState getCurrentState(String token) throws Exception {
		MvcResult result = mvc
				.perform(get("/survey/state")
						.param("token", token)
				)
				.andExpect(status().isOk())
				.andReturn();

		return om.readValue(result.getResponse().getContentAsString(), ResponseState.class);
	}

	@BeforeEach
	public void setup() throws IOException {
		surveys.deleteAll();

		students = getStudents();

		logger.info("found {} students", students.size());

		// model
		BayesianNetwork bn = new BayesianNetwork();

		// skill-chain
		// S0 -> S1 -> S2 -> S3
		//  v     v     v     v
		// Q0*   Q1*   Q2*   Q3*
		int S0 = bn.addVariable(4); // Horen
		int S1 = bn.addVariable(4); // Lesen
		int S2 = bn.addVariable(4); // Wortschatz
		int S3 = bn.addVariable(4); // Kommunikation

		bn.addParent(S1, S0);
		bn.addParent(S2, S1);
		bn.addParent(S3, S2);

		bn.setFactor(S0, new BayesianFactor(bn.getDomain(S0), new double[]{
				.15, .35, .35, .15
		}));
		bn.setFactor(S1, new BayesianFactor(bn.getDomain(S0, S1), new double[]{
				.40, .30, .20, .10,
				.25, .35, .25, .15,
				.15, .25, .35, .25,
				.10, .20, .30, .40
		}));
		bn.setFactor(S2, new BayesianFactor(bn.getDomain(S1, S2), new double[]{
				.40, .30, .20, .10,
				.25, .35, .25, .15,
				.15, .25, .35, .25,
				.10, .20, .30, .40
		}));
		bn.setFactor(S3, new BayesianFactor(bn.getDomain(S2, S3), new double[]{
				.40, .30, .20, .10,
				.25, .35, .25, .15,
				.15, .25, .35, .25,
				.10, .20, .30, .40
		}));

		// questions
		int A2 = 1, B1 = 2, B2 = 3; // there are no question of A1 difficulty...

		double[][] cpt = new double[][]{
				new double[]{ // easy
						.6125, .3875,
						.7625, .2375,
						.8625, .1375,
						.9625, .0375
				},
				new double[]{ // medium easy
						.3375, .6625,
						.6125, .3875,
						.7625, .2375,
						.8625, .1375
				},
				new double[]{ // medium hard
						.2375, .7625,
						.3375, .6625,
						.6125, .3875,
						.7625, .2375,
				},
				new double[]{ // hard
						.1875, .8125,
						.2375, .7625,
						.3375, .6625,
						.6125, .3875,
				}
		};

		// add all questions to the model
		List<Q> qs = new ArrayList<>();

		logger.info("adding question nodes");

		int i = 1;
		for (; i <= 10; i++) qs.add(addQuestion(bn, i, S0, A2, cpt));
		for (; i <= 20; i++) qs.add(addQuestion(bn, i, S0, B1, cpt));
		for (; i <= 30; i++) qs.add(addQuestion(bn, i, S0, B2, cpt));
		for (; i <= 35; i++) qs.add(addQuestion(bn, i, S1, A2, cpt));
		for (; i <= 40; i++) qs.add(addQuestion(bn, i, S1, B1, cpt));
		for (; i <= 45; i++) qs.add(addQuestion(bn, i, S1, B2, cpt));
		for (; i <= 51; i++) qs.add(addQuestion(bn, i, S2, A2, cpt));
		for (; i <= 61; i++) qs.add(addQuestion(bn, i, S2, B1, cpt));
		for (; i <= 71; i++) qs.add(addQuestion(bn, i, S2, B2, cpt));
		for (; i <= 79; i++) qs.add(addQuestion(bn, i, S3, A2, cpt));
		for (; i <= 87; i++) qs.add(addQuestion(bn, i, S3, B1, cpt));
		for (; i <= 95; i++) qs.add(addQuestion(bn, i, S3, B2, cpt));

		logger.info("added {} nodes", qs.size());

		List<String> modelData = new BayesUAIWriter(bn, "").serialize();

		// skill definition
		Skill skill0 = addSurveySkill(S0, "S0 Horen");
		Skill skill1 = addSurveySkill(S1, "S1 Lesen");
		Skill skill2 = addSurveySkill(S2, "S2 Wortschatz");
		Skill skill3 = addSurveySkill(S3, "S3 Kommunikation");

		final Map<Integer, Skill> skills = List.of(skill0, skill1, skill2, skill3).stream().collect(Collectors.toMap(Skill::getVariable, v -> v));

		final List<Question> questionList = qs.stream()
				.map(q -> new Question()
						.setExplanation(q.idx)
						.setQuestion(q.toString())
						.setSkill(skills.get(q.skill))
						.setLevel("" + q.difficulty)
						.setVariable(q.q)
						.addAnswersAvailable(
								new QuestionAnswer().setText("0").setState(0),
								new QuestionAnswer().setText("1").setState(1)
						)
				)
				.collect(Collectors.toList());

		questions.saveAll(questionList);

		Survey survey = new Survey()
				.setAccessCode(ACCESS_CODE)
				.setDescription("This is based on an assessment test for the German language.")
				.setDuration(3600L)
				.setQuestions(questionList)
				.setSkillOrder(List.of(skill0.getName(), skill1.getName(), skill2.getName(), skill3.getName()))
				.setModelData(String.join("\n", modelData))
				.setMixedSkillOrder(false)
				.setIsAdaptive(true)
				.setEntropyLowerThreshold(ENTROPY_STOP_THRESHOLD_MIN)
				.setEntropyUpperThreshold(ENTROPY_STOP_THRESHOLD_MAX);

		surveys.save(survey);

		logger.info("Added new survey with accessCode={}", ACCESS_CODE);
	}

	@Ignore
	@Test
	public void testAdaptiveOneStudent() throws Exception {
		final Student student = students.get(FIRST_STUDENT);
		final String token = init();

		ResponseQuestion nextQuestion;

		while ((nextQuestion = getNextQuestion(token)) != null) {
			postAnswer(token,
					nextQuestion.id, // this is an answer to this question
					nextQuestion.answers.get(student.get(nextQuestion.explanation)).id // 0 is always correct 1 is always wrong
			);
		}

		ResponseState state = getCurrentState(token);

		StringBuilder sb = new StringBuilder();
		sb.append("Total answers:").append(state.totalAnswers).append("\n");
		for (Skill skill : state.skills) {
			final String s = skill.getName();
			sb.append("Skill: ").append(s).append("\n")
					.append("\tQuestions: ").append(state.questionsPerSkill.get(s)).append("\n")
					.append("\tLevel:     ").append(skill.getStates().get(argmax(state.skillDistribution.get(s))).getName()).append("\n")
					.append("\tEntropy:   ").append(state.entropyDistribution.get(s)).append("\n")
			;
		}
		Files.write(Paths.get("student." + FIRST_STUDENT + ".txt"), sb.toString().getBytes());

		logger.info("Saved to {} as number {}", Paths.get("").toAbsolutePath(), FIRST_STUDENT);

	}

	@Ignore
	@Test
	public void testAdaptiveMultipleStudent() throws Exception {
		logger.info("Multiple students from {} to {} over {} core(s)", FIRST_STUDENT, LAST_STUDENT, CORES);

		ExecutorService es = Executors.newFixedThreadPool(CORES);

		final List<Callable<Void>> tasks = IntStream.range(Math.max(FIRST_STUDENT, 0), Math.min(LAST_STUDENT, students.size()))
				.mapToObj(students::get)
				.map(student -> (Callable<Void>) () -> {
					final String token = init();

					ResponseQuestion nextQuestion;
					while ((nextQuestion = getNextQuestion(token)) != null) {
						postAnswer(token,
								nextQuestion.id, // this is an answer to this question
								nextQuestion.answers.get(student.get(nextQuestion.explanation)).id // 0 is always correct 1 is always wrong
						);
					}

					student.state = getCurrentState(token);
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
				final List<String> header = state.skills.stream().map(Skill::getName).collect(Collectors.toList());
				lines.add(String.join("\t", header));
			}

			final List<String> line = state.skills.stream()
					.map(skill -> skill.getStates().get(argmax(state.skillDistribution.get(skill.getName()))).getName())
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
	 * Dummy class to identify a network node for a question.
	 */
	@AllArgsConstructor
	static class Q {
		int q, skill, difficulty;
		String idx;

		@Override
		public String toString() {
			return "Q{" +
					"q=" + q +
					", skill=" + skill +
					", difficulty=" + difficulty +
					'}';
		}
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
