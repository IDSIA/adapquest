package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.persistence.external.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

		final BayesianFactor fS0 = BayesianFactorFactory.factory().domain(bn.getDomain(s0)).data(new double[]{.5, .5}).get();
		final BayesianFactor fQ0 = BayesianFactorFactory.factory().domain(bn.getDomain(s0, q0)).data(new double[]{.1, .9, .9, .1}).get();
		final BayesianFactor fQ1 = BayesianFactorFactory.factory().domain(bn.getDomain(s0, q1)).data(new double[]{.51, .49, .49, .51}).get();

		bn.setFactor(s0, fS0);
		bn.setFactor(q0, fQ0);
		bn.setFactor(q1, fQ1);

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

		final BayesianFactor fS0 = BayesianFactorFactory.factory().domain(bn.getDomain(s0)).data(new double[]{.4, .6}).get();
		final BayesianFactor fS1 = BayesianFactorFactory.factory().domain(bn.getDomain(s0, s1)).data(new double[]{.3, .7, .6, .4}).get();
		final BayesianFactor fQ0 = BayesianFactorFactory.factory().domain(bn.getDomain(s0, q0)).data(new double[]{.2, .8, .3, .7}).get();
		final BayesianFactor fQ1 = BayesianFactorFactory.factory().domain(bn.getDomain(s1, q1)).data(new double[]{.6, .4, .4, .6}).get();

		bn.setFactor(s0, fS0);
		bn.setFactor(s1, fS1);
		bn.setFactor(q0, fQ0);
		bn.setFactor(q1, fQ1);

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

		final BayesianFactor fS0 = BayesianFactorFactory.factory().domain(bn.getDomain(s0)).data(new double[]{.51, .49}).get();
		final BayesianFactor fS1 = BayesianFactorFactory.factory().domain(bn.getDomain(s0, s1)).data(new double[]{.4, .6, .7, .3}).get();

		bn.setFactor(s0, fS0);
		bn.setFactor(s1, fS1);

		final List<SkillStructure> skillStructure = List.of(skill(s0, "S0"), skill(s1, "S1"));

		final List<QuestionStructure> questionStructure = new ArrayList<>();
		int i = 1;
		for (; i <= 5; i++) {
			final int q = bn.addVariable(2);
			bn.addParent(q, s0);
			double r = i * .05;
			double w = i * .08;

			final BayesianFactor f = BayesianFactorFactory.factory().domain(bn.getDomain(s0, q)).data(new double[]{r, 1 - r, 1 - w, w}).get();
			bn.setFactor(q, f);

			questionStructure.add(question(q, "Q" + i, "S0"));
		}
		for (; i <= 10; i++) {
			final int q = bn.addVariable(2);
			bn.addParent(q, s0);
			double r = i * .08;
			double w = i * .05;

			final BayesianFactor f = BayesianFactorFactory.factory().domain(bn.getDomain(s0, q)).data(new double[]{r, 1 - r, 1 - w, w}).get();
			bn.setFactor(q, f);

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

		final BayesianFactor fS0 = BayesianFactorFactory.factory().domain(bn.getDomain(s0)).data(new double[]{.51, .49}).get();
		bn.setFactor(s0, fS0);

		final List<SkillStructure> skillStructure = List.of(skill(s0, "S0"));

		Random r = new Random(7);

		final List<QuestionStructure> questionStructure = new ArrayList<>();
		int i = 1;
		for (; i <= 20; i++) {
			final int q = bn.addVariable(2);
			bn.addParent(q, s0);
			double v = r.nextDouble();
			double w = r.nextDouble();

			final BayesianFactor f = BayesianFactorFactory.factory().domain(bn.getDomain(s0, q)).data(new double[]{v, 1 - v, 1 - w, w}).get();
			bn.setFactor(q, f);

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

	public static ImportStructure structure3S7QMultiChoice(String code) {
		final BayesianNetwork bn = new BayesianNetwork();
		// skill nodes
		final int[] s = {
				bn.addVariable(2), // s0
				bn.addVariable(2), // s1
				bn.addVariable(2), // s2
		};
		// answer nodes, a question is composed by answers
		final int[] a = {
				bn.addVariable(2), // a0 q0
				bn.addVariable(2), // a1 q0
				bn.addVariable(2), // a2 q1
				bn.addVariable(2), // a3 q1
				bn.addVariable(2), // a4 q1
				bn.addVariable(2), // a5 q2
				bn.addVariable(2), // a6 q2
		};
		// parents for answer nodes
		final int[][] p = {
				{s[0], s[1]}, // Pa(a0)
				{s[0], s[1]}, // Pa(a1)
				{s[1], s[2]}, // Pa(a2)
				{s[1], s[2]}, // Pa(a3)
				{s[1], s[2]}, // Pa(a4)
				{s[0], s[2]}, // Pa(a5)
				{s[1], s[2]}, // Pa(a6)
		};
		// set parents
		for (int i = 0; i < a.length; i++)
			bn.addParents(a[i], p[i]);

		// noisy-or inhibitors
		final double[][] inh = {
				{.1, .2}, // Inh(a0)
				{.3, .4}, // Inh(a1)
				{.2, .3}, // Inh(a2)
				{.1, .2}, // Inh(a3)
				{.5, .6}, // Inh(a4)
				{.1, .4}, // Inh(a5)
				{.5, .7}, // Inh(a6)
		};
		// answer nodes for each question
		final int[][] q = {
				{0, 1},     // q0
				{2, 3, 4},  // q1
				{5, 6},     // q2
		};

		final BayesianFactor[] f = new BayesianFactor[s.length + a.length];

		for (int i = 0; i < s.length; i++)
			// factors for skills
			f[i] = BayesianFactorFactory.factory().domain(bn.getDomain(s[i])).data(new double[]{.5, .5}).get();
		for (int i = 0, j = s.length; i < a.length; i++, j++)
			// factors for answer/questions
			f[j] = BayesianFactorFactory.factory().domain(bn.getDomain(a[i], p[i][0], p[i][1])).noisyOr(bn.getParents(a[i]), inh[i]);

		bn.setFactors(f);

		// build survey model structure
		final List<SkillStructure> skillStructures = IntStream.range(0, s.length)
				.mapToObj(i -> skill(i, "S" + i))
				.collect(Collectors.toList());

		final List<QuestionStructure> questionStructures = IntStream.range(0, q.length)
				.mapToObj(i -> {
							final Set<String> skills = new HashSet<>();
							final List<AnswerStructure> answers = new ArrayList<>();

					// each question is composed by N*2 answers
							for (int aq : q[i]) {
								for (int pa : p[aq])
									skills.add(skillStructures.get(pa).getName());
								answers.add(new AnswerStructure("no", 0).setVariable(a[aq]));
								answers.add(new AnswerStructure("yes", 1).setVariable(a[aq]));
							}

							return new QuestionStructure()
									.setName("Q" + i)
									.setMultipleChoice(true)
									.setMultipleSkills(true)
									.setSkills(skills)
									.setAnswers(answers);
						}
				)
				.collect(Collectors.toList());

		final String modelData = String.join("\n", new BayesUAIWriter(bn, "").serialize());
		final SurveyStructure surveyStructure = new SurveyStructure()
				.setAccessCode(code)
				.setAdaptive(true)
				.setSimple(true)
				.setQuestionTotalMin(3);

		return new ImportStructure()
				.setSurvey(surveyStructure)
				.setSkills(skillStructures)
				.setQuestions(questionStructures)
				.setModelData(modelData);
	}
}
