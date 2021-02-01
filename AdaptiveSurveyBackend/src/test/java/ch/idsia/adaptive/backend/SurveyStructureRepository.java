package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.persistence.external.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;

import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    01.02.2021 14:39
 */
public class SurveyStructureRepository {

	public static ImportStructure structure2Q2S(String code) {
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

		final SurveyStructure surveyStructure = new SurveyStructure().setAccessCode(code);

		return new ImportStructure()
				.setSurvey(surveyStructure)
				.setSkills(skillStructure)
				.setQuestions(questionStructure)
				.setModelData(modelData);
	}

}
