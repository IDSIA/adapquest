package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.persistence.external.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    01.02.2021 14:39
 */
public class SurveyStructureRepository {

	private static QuestionStructure question(int variable, String name, String skill) {
		return new QuestionStructure().setSkill(skill).setName(name).setVariable(variable).setAnswers(List.of(
				new AnswerStructure("0", 0),
				new AnswerStructure("1", 1)
		));
	}

	private static SkillStructure skill(int variable, String name) {
		return new SkillStructure().setName(name).setVariable(variable).setStates(List.of(
				new StateStructure(name + 0, 0),
				new StateStructure(name + 1, 1)
		));
	}

	public static ImportStructure structure1S2Q(String code) {
		final BayesianNetwork bn = new BayesianNetwork();
		final int s0 = bn.addVariable(2);
		final int q0 = bn.addVariable(2);
		final int q1 = bn.addVariable(2);

		bn.addParent(q0, s0);
		bn.addParent(q1, s0);

		bn.setFactor(s0, new BayesianFactor(bn.getDomain(s0), new double[]{.5, .5}));
		bn.setFactor(q0, new BayesianFactor(bn.getDomain(s0, q0), new double[]{.1, .9, .9, .1}));
		bn.setFactor(q1, new BayesianFactor(bn.getDomain(s0, q1), new double[]{.51, .49, .49, .51}));

		final String modelData = String.join("\n", new BayesUAIWriter(bn, "").serialize());

		final List<SkillStructure> skillStructure = List.of(skill(s0, "S0"));
		final List<QuestionStructure> questionStructure = List.of(
				question(q0, "Q0", "S0"),
				question(q1, "Q1", "S0")
		);

		final SurveyStructure surveyStructure = new SurveyStructure().setAccessCode(code).setAdaptive(true);

		return new ImportStructure()
				.setSurvey(surveyStructure)
				.setSkills(skillStructure)
				.setQuestions(questionStructure)
				.setModelData(modelData);
	}

	public static ImportStructure structure2S2Q(String code) {
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
				skill(s0, "S0"),
				skill(s1, "S1")
		);

		final List<QuestionStructure> questionStructure = List.of(
				question(q0, "Q0", "S0"),
				question(q1, "Q1", "S1")
		);

		final SurveyStructure surveyStructure = new SurveyStructure().setAccessCode(code).setAdaptive(true);

		return new ImportStructure()
				.setSurvey(surveyStructure)
				.setSkills(skillStructure)
				.setQuestions(questionStructure)
				.setModelData(modelData);
	}

	public static ImportStructure structure2S10Q(String code) {
		final BayesianNetwork bn = new BayesianNetwork();
		final int s0 = bn.addVariable(2);
		final int s1 = bn.addVariable(2);
		bn.addParent(s1, s0);
		bn.setFactor(s0, new BayesianFactor(bn.getDomain(s0), new double[]{.51, .49}));
		bn.setFactor(s1, new BayesianFactor(bn.getDomain(s0, s1), new double[]{.4, .6, .7, .3}));

		final List<SkillStructure> skillStructure = List.of(skill(s0, "S0"), skill(s1, "S1"));

		final List<QuestionStructure> questionStructure = new ArrayList<>();
		int i = 1;
		for (; i <= 5; i++) {
			final int q = bn.addVariable(2);
			bn.addParent(q, s0);
			double r = i * .05;
			double w = i * .08;
			bn.setFactor(q, new BayesianFactor(bn.getDomain(s0, q), new double[]{r, 1 - r, 1 - w, w}));

			questionStructure.add(question(q, "Q" + i, "S0"));
		}
		for (; i <= 10; i++) {
			final int q = bn.addVariable(2);
			bn.addParent(q, s0);
			double r = i * .08;
			double w = i * .05;
			bn.setFactor(q, new BayesianFactor(bn.getDomain(s0, q), new double[]{r, 1 - r, 1 - w, w}));

			questionStructure.add(question(q, "Q" + i, "S1"));
		}

		final String modelData = String.join("\n", new BayesUAIWriter(bn, "").serialize());
		final SurveyStructure surveyStructure = new SurveyStructure().setAccessCode(code).setAdaptive(true);

		return new ImportStructure()
				.setSurvey(surveyStructure)
				.setSkills(skillStructure)
				.setQuestions(questionStructure)
				.setModelData(modelData);
	}

	public static ImportStructure structure1S20Q(String code) {
		final BayesianNetwork bn = new BayesianNetwork();
		final int s0 = bn.addVariable(2);
		bn.setFactor(s0, new BayesianFactor(bn.getDomain(s0), new double[]{.51, .49}));

		final List<SkillStructure> skillStructure = List.of(skill(s0, "S0"));

		Random r = new Random(7);

		final List<QuestionStructure> questionStructure = new ArrayList<>();
		int i = 1;
		for (; i <= 20; i++) {
			final int q = bn.addVariable(2);
			bn.addParent(q, s0);
			double v = r.nextDouble();
			double w = r.nextDouble();
			bn.setFactor(q, new BayesianFactor(bn.getDomain(s0, q), new double[]{v, 1 - v, 1 - w, w}));

			questionStructure.add(question(q, "Q" + i, "S0"));
		}

		final String modelData = String.join("\n", new BayesUAIWriter(bn, "").serialize());
		final SurveyStructure surveyStructure = new SurveyStructure().setAccessCode(code).setAdaptive(true);

		return new ImportStructure()
				.setSurvey(surveyStructure)
				.setSkills(skillStructure)
				.setQuestions(questionStructure)
				.setModelData(modelData);
	}

}
