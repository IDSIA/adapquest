package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.TestApplication;
import ch.idsia.adaptive.backend.config.PersistenceConfig;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.external.*;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIParser;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    19.01.2021 13:12
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {
		TestApplication.class,
		PersistenceConfig.class,
		SurveyRepository.class,
		InitializationService.class,
})
class InitializationServiceTest {

	@Autowired
	InitializationService is;

	@Autowired
	SurveyRepository surveys;

	@Test
	public void testLoadFromDataFolder() {

		is.readDataFolder();

		assertEquals(2, surveys.count());

		final Survey survey = surveys.findByAccessCode("NonAdaptiveSurvey-Example");

		assertNotNull(survey);
		assertEquals(1, survey.getSkillOrder().size());
		assertEquals(3, survey.getQuestions().size());

		assertEquals(3, survey.getQuestions().get(0).getAnswersAvailable().size());
		assertEquals(2, survey.getQuestions().get(1).getAnswersAvailable().size());
		assertEquals(3, survey.getQuestions().get(2).getAnswersAvailable().size());

		List<String> lines = Arrays.stream(survey.getModelData().split("\n")).collect(Collectors.toList());

		BayesianNetwork bn = new BayesUAIParser(lines).parse();

		assertEquals(4, bn.getVariables().length);
	}

	@Test
	public void testLoadFromStructureWithModelData() {
		final BayesianNetwork bn = new BayesianNetwork();
		final int s0 = bn.addVariable(2);
		final int s1 = bn.addVariable(2);
		final int q0 = bn.addVariable(2);
		final int q1 = bn.addVariable(2);

		bn.addParent(s1, s0);
		bn.addParent(q0, s0);
		bn.addParent(q1, s1);

		bn.setFactor(s0, new BayesianFactor(bn.getDomain(s0), new double[]{.4, .6}));
		bn.setFactor(s1, new BayesianFactor(bn.getDomain(s0, s1), new double[]{.3, .7, .6, .4}));
		bn.setFactor(q0, new BayesianFactor(bn.getDomain(s0, q0), new double[]{.2, .8, .3, .7}));
		bn.setFactor(q1, new BayesianFactor(bn.getDomain(s1, q1), new double[]{.6, .4, .4, .6}));

		final String modelData = String.join("\n", new BayesUAIWriter(bn, "").serialize());

		final List<SkillStructure> skillStructure = List.of(
				new SkillStructure().setName("S0").setVariable(s0).setStates(List.of(
						new StateStructure("s00", 0),
						new StateStructure("s01", 1)
				)),
				new SkillStructure().setName("S1").setVariable(s1).setStates(List.of(
						new StateStructure("s10", 0),
						new StateStructure("s11", 1)
				))
		);

		final List<QuestionStructure> questionStructure = List.of(
				new QuestionStructure().setSkill("S0").setName("Q0").setVariable(q0).setAnswers(List.of(
						new AnswerStructure("0", 0, false),
						new AnswerStructure("1", 1, false)
				)),
				new QuestionStructure().setSkill("S1").setName("Q1").setVariable(q1).setAnswers(List.of(
						new AnswerStructure("0", 0, false),
						new AnswerStructure("1", 1, false)
				))
		);

		final SurveyStructure surveyStructure = new SurveyStructure().setAccessCode("test");

		ImportStructure structure = new ImportStructure()
				.setSurvey(surveyStructure)
				.setSkills(skillStructure)
				.setQuestions(questionStructure)
				.setModelData(modelData);

		is.parseSurvey(structure);

		final Survey survey = surveys.findByAccessCode("test");
		survey.getQuestions().forEach(q -> {
					assertTrue(q.getVariable() >= 0);
					assertTrue(q.getSkill().getVariable() >= 0);
				}
		);
	}
}