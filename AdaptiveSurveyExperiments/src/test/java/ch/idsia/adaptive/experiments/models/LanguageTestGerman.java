package ch.idsia.adaptive.experiments.models;

import ch.idsia.adaptive.backend.persistence.external.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    29.01.2021 17:56
 */
public class LanguageTestGerman {
	private static final Logger logger = LogManager.getLogger(LanguageTestGerman.class);

	public static final String accessCode = "LanguageTestGerman";

	public Double ENTROPY_STOP_THRESHOLD_MAX = .0;
	public Double ENTROPY_STOP_THRESHOLD_MIN = .25;

	public ImportStructure structure() {
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

		String modelData = String.join("\n", new BayesUAIWriter(bn, "").serialize());

		// skill definition
		SkillStructure skill0 = addSurveySkill(S0, "S0 Horen");
		SkillStructure skill1 = addSurveySkill(S1, "S1 Lesen");
		SkillStructure skill2 = addSurveySkill(S2, "S2 Wortschatz");
		SkillStructure skill3 = addSurveySkill(S3, "S3 Kommunikation");

		final List<SkillStructure> skills = List.of(skill0, skill1, skill2, skill3);
		final Map<Integer, String> skillVarToInt = skills.stream().collect(Collectors.toMap(
				SkillStructure::getVariable, SkillStructure::getName
		));

		final List<QuestionStructure> questions = qs.stream()
				.map(q -> new QuestionStructure()
						.setSkill(skillVarToInt.get(q.skill))
						.setQuestion("" + q.toString())
						.setExplanation("Q" + q.idx)
						.setVariable(q.idx)
						.setAnswers(List.of(
								new AnswerStructure().setText("0").setState(0),
								new AnswerStructure().setText("1").setState(1)
						))
				)
				.collect(Collectors.toList());

		SurveyStructure survey = new SurveyStructure()
				.setAccessCode(accessCode)
				.setLanguage("de")
				.setDescription("This is based on an assessment test for the German language.")
				.setDuration(3600L)
				.setSkillOrder(List.of(skill0.getName(), skill1.getName(), skill2.getName(), skill3.getName()))
				.setMixedSkillOrder(false)
				.setAdaptive(true)
				.setEntropyLowerThreshold(ENTROPY_STOP_THRESHOLD_MIN)
				.setEntropyUpperThreshold(ENTROPY_STOP_THRESHOLD_MAX);

		logger.info("Created new survey with accessCode={}", accessCode);
		return new ImportStructure()
				.setSurvey(survey)
				.setSkills(skills)
				.setQuestions(questions)
				.setModelData(Optional.of(modelData));
	}

	private Q addQuestion(BayesianNetwork bn, int idx, int skill, int difficulty, double[][] data) {
		logger.info("Adding to network question node={} difficulty={} for skill={}", idx, difficulty, skill);
		int q = bn.addVariable(2);
		bn.addParent(q, skill);
		bn.setFactor(q, new BayesianFactor(bn.getDomain(skill, q), data[difficulty]));
		return new Q(q, skill, difficulty, "Q" + idx);
	}

	private SkillStructure addSurveySkill(int variable, String name) {
		logger.info("Adding skill {}", name);
		return new SkillStructure()
				.setName(name)
				.setVariable(variable)
				.setStates(List.of(
						new StateStructure("A1", 0),
						new StateStructure("A2", 1),
						new StateStructure("A3", 2),
						new StateStructure("A4", 3)
				));
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

}
