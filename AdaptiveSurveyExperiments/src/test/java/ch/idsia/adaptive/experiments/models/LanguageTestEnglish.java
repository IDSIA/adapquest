package ch.idsia.adaptive.experiments.models;

import ch.idsia.adaptive.backend.persistence.external.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
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
public class LanguageTestEnglish extends AbstractAdaptiveModel {
	private static final Logger logger = LogManager.getLogger(LanguageTestEnglish.class);

	public static final String accessCode = "LanguageTestGerman";

	int S0, S1, S2;

	SkillStructure skill0, skill1, skill2;

	List<Question> Qs;
	Map<Integer, String> skillVarToInt;

	public LanguageTestEnglish() {
		super(accessCode);
	}

	@Override
	public String model() {
		BayesianNetwork bn = new BayesianNetwork();

		// skill-chain
		// S0 -> S1 -> S2 -> S3
		//  v     v     v     v
		// Q0*   Q1*   Q2*   Q3*
		S0 = bn.addVariable(4); // Reading
		S1 = bn.addVariable(4); // Grammar
		S2 = bn.addVariable(4); // Listening

		bn.addParent(S1, S0);
		bn.addParent(S2, S1);

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


		// questions
		int A1 = 0, A2 = 1, B1 = 2, B2 = 3;

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
		for (; i <= 16; i++) Qs.add(addQuestion(bn, i, S0, B1, cpt));
		for (; i <= 36; i++) Qs.add(addQuestion(bn, i, S1, A1, cpt));
		for (; i <= 51; i++) Qs.add(addQuestion(bn, i, S1, A2, cpt));
		for (; i <= 66; i++) Qs.add(addQuestion(bn, i, S1, B1, cpt));
		for (; i <= 81; i++) Qs.add(addQuestion(bn, i, S1, B2, cpt));
		for (; i <= 100; i++) Qs.add(addQuestion(bn, i, S2, B1, cpt));

		logger.info("added {} nodes", Qs.size());

		return String.join("\n", new BayesUAIWriter(bn, "").serialize());
	}

	@Override
	public List<SkillStructure> skills() {
		skill0 = addSurveySkill(S0, "S0 Reading");
		skill1 = addSurveySkill(S1, "S1 Grammar");
		skill2 = addSurveySkill(S2, "S2 Listening");

		final List<SkillStructure> skills = List.of(skill0, skill1, skill2);
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
				.setSkillOrder(List.of(skill0.getName(), skill1.getName(), skill2.getName()))
				.setMixedSkillOrder(false)
				.setAdaptive(true);
	}

	private Question addQuestion(BayesianNetwork bn, int idx, int skill, int difficulty, double[][] data) {
		logger.info("Adding to network question node={} difficulty={} for skill={}", idx, difficulty, skill);
		int q = bn.addVariable(2);
		bn.addParent(q, skill);
		bn.setFactor(q, new BayesianFactor(bn.getDomain(skill, q), data[difficulty]));
		return new Question(q, skill, difficulty, "Q" + idx);
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

}
