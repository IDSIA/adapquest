package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.config.WebConfig;
import ch.idsia.adaptive.backend.controller.SurveyController;
import ch.idsia.adaptive.backend.persistence.dao.QuestionRepository;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.services.InitializationService;
import ch.idsia.adaptive.backend.services.SessionService;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.transaction.Transactional;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    26.01.2021 13:42
 */
@RunWith(SpringRunner.class)
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

	private static final String ACCESS_CODE = "LanguageTestGerman";
	@Autowired
	MockMvc mvc;
	@Autowired
	SurveyRepository surveys;
	@Autowired
	QuestionRepository questions;
	List<Student> students;

	private Skill addSurveySkill(int variable, String name) {
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
		int q = bn.addVariable(2);
		bn.addParent(q, skill);
		bn.setFactor(q, new BayesianFactor(bn.getDomain(skill, q), data[difficulty]));
		return new Q(q, skill, difficulty, "Q" + idx);
	}

	private List<Student> getStudents() throws IOException {
		try (BufferedReader br = new BufferedReader(new InputStreamReader(TestAdaptiveLanguage.class.getResourceAsStream("/languageTestAnswers.csv")))) {
			final List<String[]> answers = br.lines().map(x -> x.split(",")).collect(Collectors.toList());

			final String[] header = answers.get(0);
			return IntStream.range(1, answers.size())
					.mapToObj(answers::get)
					.map(a -> new Student(header, a))
					.collect(Collectors.toList());

		}
	}

	@BeforeEach
	public void setup() throws IOException {
		students = getStudents();

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

		bn.setFactor(S0, new BayesianFactor(bn.getDomain(S1), new double[]{
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
		int A1 = 0, A2 = 1, B1 = 2, B2 = 3;

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

		List<String> modelData = new BayesUAIWriter(bn, "").serialize();

		// skill definition
		Skill skill0 = addSurveySkill(S0, "Horen");
		Skill skill1 = addSurveySkill(S1, "Lesen");
		Skill skill2 = addSurveySkill(S2, "Wortschatz");
		Skill skill3 = addSurveySkill(S3, "Kommunikation");

		final Map<Integer, Skill> skills = List.of(skill0, skill1, skill2, skill3).stream().collect(Collectors.toMap(Skill::getVariable, v -> v));

		final List<Question> questionList = qs.stream()
				.map(q -> new Question()
						.setExplanation(q.idx)
						.setQuestion(q.toString())
						.setSkill(skills.get(q.skill))
						.setLevel("" + q.difficulty)
						.setVariable(q.q)
						.addAnswersAvailable(
								new QuestionAnswer().setText("A").setState(0),
								new QuestionAnswer().setText("B").setState(1)
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
				.setIsAdaptive(true);

		surveys.save(survey);
	}

	@Test
	public void testAdaptiveOneStudent() throws Exception {

		MvcResult result;
		result = mvc
				.perform(get("/survey/init")
						.param("accessCode", ACCESS_CODE)
				)
				.andExpect(status().isOk())
				.andReturn();

	}

	@Data
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

	static class Student extends HashMap<String, Integer> {

		public Student(String[] header, String[] answers) {
			IntStream.range(0, header.length)
					.forEach(i -> put(header[i], Integer.parseInt(answers[i])));
		}
	}

}
