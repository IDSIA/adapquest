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
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    29.01.2021 17:56
 */
public class LanguageTestGerman extends AbstractAdaptiveModel {
	private static final Logger logger = LogManager.getLogger(LanguageTestGerman.class);

	public static final String accessCode = "LanguageTestGerman";

	int S0, S1, S2, S3;

	SkillStructure skill0, skill1, skill2, skill3;

	List<Q> Qs;
	Map<Integer, String> skillVarToInt;

	public LanguageTestGerman() {
		super(accessCode);
	}

	@Override
	public String model() {
		BayesianNetwork bn = new BayesianNetwork();

		// skill-chain
		// S0 -> S1 -> S2 -> S3
		//  v     v     v     v
		// Q0*   Q1*   Q2*   Q3*
		S0 = bn.addVariable(4); // Hoeren
		S1 = bn.addVariable(4); // Lesen
		S2 = bn.addVariable(4); // Wortschatz
		S3 = bn.addVariable(4); // Kommunikation

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
						.3875, .6125,
						.2375, .7625,
						.1375, .8625,
						.0375, .9625,
				},
				new double[]{ // medium easy
						.6625, .3375,
						.3875, .6125,
						.2375, .7625,
						.1375, .8625,
				},
				new double[]{ // medium hard
						.7625, .2375,
						.6625, .3375,
						.3875, .6125,
						.2375, .7625,
				},
				new double[]{ // hard
						.8125, .1875,
						.7625, .2375,
						.6625, .3375,
						.3875, .6125,
				}
		};

		// add all questions to the model
		Qs = new ArrayList<>();

		logger.info("adding question nodes");

		int i = 1;
		for (; i <= 10; i++) Qs.add(addQuestion(bn, i, S0, A2, cpt));
		for (; i <= 20; i++) Qs.add(addQuestion(bn, i, S0, B1, cpt));
		for (; i <= 30; i++) Qs.add(addQuestion(bn, i, S0, B2, cpt));
		for (; i <= 35; i++) Qs.add(addQuestion(bn, i, S1, A2, cpt));
		for (; i <= 40; i++) Qs.add(addQuestion(bn, i, S1, B1, cpt));
		for (; i <= 45; i++) Qs.add(addQuestion(bn, i, S1, B2, cpt));
		for (; i <= 51; i++) Qs.add(addQuestion(bn, i, S2, A2, cpt));
		for (; i <= 61; i++) Qs.add(addQuestion(bn, i, S2, B1, cpt));
		for (; i <= 71; i++) Qs.add(addQuestion(bn, i, S2, B2, cpt));
		for (; i <= 79; i++) Qs.add(addQuestion(bn, i, S3, A2, cpt));
		for (; i <= 87; i++) Qs.add(addQuestion(bn, i, S3, B1, cpt));
		for (; i <= 95; i++) Qs.add(addQuestion(bn, i, S3, B2, cpt));

		logger.info("added {} nodes", Qs.size());

		return String.join("\n", new BayesUAIWriter(bn, "").serialize());
	}

	@Override
	public List<SkillStructure> skills() {
		skill0 = addSurveySkill(S0, "S0 Hoeren");
		skill1 = addSurveySkill(S1, "S1 Lesen");
		skill2 = addSurveySkill(S2, "S2 Wortschatz");
		skill3 = addSurveySkill(S3, "S3 Kommunikation");

		final List<SkillStructure> skills = List.of(skill0, skill1, skill2, skill3);
		skillVarToInt = skills.stream().collect(Collectors.toMap(
				SkillStructure::getVariable, SkillStructure::getName
		));

		return skills;
	}

	@Override
	public List<QuestionStructure> questions() {
		return Qs.stream()
				.map(q -> new QuestionStructure()
						.setSkill(skillVarToInt.get(q.skill))
						.setQuestion(q.toString())
						.setExplanation(q.idx)
						.setName(q.idx)
						.setVariable(q.q)
						.setAnswers(List.of(
								new AnswerStructure("0", 0),
								new AnswerStructure("1", 1)
						))
				)
				.collect(Collectors.toList());
	}

	@Override
	public SurveyStructure survey() {
		return super.survey()
				.setLanguage("de")
				.setDescription("This is based on an assessment test for the German language.")
				.setDuration(3600L)
				.setSkillOrder(List.of(skill0.getName(), skill1.getName(), skill2.getName(), skill3.getName()))
				.setMixedSkillOrder(false)
				.setAdaptive(true);
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
