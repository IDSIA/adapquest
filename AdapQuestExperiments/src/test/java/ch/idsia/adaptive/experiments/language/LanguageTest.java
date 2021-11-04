package ch.idsia.adaptive.experiments.language;

import ch.idsia.adaptive.backend.persistence.external.*;
import ch.idsia.adaptive.experiments.models.AbstractAdaptiveModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    29.01.2021 17:56
 */
public class LanguageTest extends AbstractAdaptiveModel {
	private static final Logger logger = LoggerFactory.getLogger(LanguageTest.class);

	public static final String accessCode = "LanguageTest";

	int S0, S1, S2, S3;
	BayesianNetwork bn;

	SkillStructure skill0, skill1, skill2, skill3;

	List<Question> Qs;
	Map<Integer, String> skillVarToInt;

	public LanguageTest() {
		super(accessCode);
	}

	@Override
	public String model() {
		// here we define the model
		bn = new BayesianNetwork();

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

		// CPT for the skills
		final BayesianFactor bf0 = BayesianFactorFactory.factory()
				.domain(bn.getDomain(S0))
				.data(new double[]{
						.15, .35, .35, .15
				})
				.get();
		final BayesianFactor bf1 = BayesianFactorFactory.factory()
				.domain(bn.getDomain(S0, S1))
				.data(new double[]{
						.40, .30, .20, .10,
						.25, .35, .25, .15,
						.15, .25, .35, .25,
						.10, .20, .30, .40
				})
				.get();
		final BayesianFactor bf2 = BayesianFactorFactory.factory()
				.domain(bn.getDomain(S1, S2))
				.data(new double[]{
						.40, .30, .20, .10,
						.25, .35, .25, .15,
						.15, .25, .35, .25,
						.10, .20, .30, .40
				})
				.get();
		final BayesianFactor bf3 = BayesianFactorFactory.factory()
				.domain(bn.getDomain(S2, S3))
				.data(new double[]{
						.40, .30, .20, .10,
						.25, .35, .25, .15,
						.15, .25, .35, .25,
						.10, .20, .30, .40
				})
				.get();

		bn.setFactor(S0, bf0);
		bn.setFactor(S1, bf1);
		bn.setFactor(S2, bf2);
		bn.setFactor(S3, bf3);

		// CPT for questions
		int A2 = 1, B1 = 2, B2 = 3; // there are no question of A1 difficulty...

		double[][] cpt = new double[][]{
				new double[]{ // easy
						.3875, .2375, .1375, .0375,
						.6125, .7625, .8625, .9625,
				},
				new double[]{ // medium easy
						.6625, .3875, .2375, .1375,
						.3375, .6125, .7625, .8625,
				},
				new double[]{ // medium hard
						.7625, .6625, .3875, .2375,
						.2375, .3375, .6125, .7625,
				},
				new double[]{ // hard
						.8125, .7625, .6625, .3875,
						.1875, .2375, .3375, .6125,
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

		// mapping skill variable index in the Bayesian model to their name in the survey
		skillVarToInt = skills.stream().collect(Collectors.toMap(
				SkillStructure::getVariable, SkillStructure::getName
		));

		return skills;
	}

	@Override
	public List<QuestionStructure> questions() {
		// converting all Questions in Question Structure
		return Qs.stream()
				.map(q -> new QuestionStructure()
						.setSkill(skillVarToInt.get(q.skill)) // this is the NAME of the skill
						.setQuestion(q.toString()) // this is just a dummy
						.setExplanation(q.idx) // this is just an hack, can be omitted
						.setName(q.idx) // Q# where # is just an identifier, not related with the Bayesian model
						.setAnswers(List.of(
								// same number of states as the question nodes in the Bayesian model
								new AnswerStructure("0", q.q, 0), // q.q is the variable index of the Bayesian model
								new AnswerStructure("1", q.q, 1)
						))
				)
				.collect(Collectors.toList());
	}

	@Override
	public SurveyStructure survey() {
		return super.survey()
				.setLanguage("de")
				.setDescription("This is based on an language level assessment test.")
				.setDuration(3600L)
				.setSkillOrder(List.of(skill0.getName(), skill1.getName(), skill2.getName(), skill3.getName()))
				.setMixedSkillOrder(false)
				.setAdaptive(true) // default is false!
				.setQuestionPerSkillMin(2) // at least 2 questions will be done for each skill
				.setScoreLowerThreshold(.5) // if score is below this threshold, then stop
				.setQuestionTotalMax(45) // TODO: remove
				;
	}

	/**
	 * Utility method that simplify adding a new question to the survey, both as a variable of the model and as an entry
	 * for the {@link QuestionStructure} entry.
	 *
	 * @param bn         {@link BayesianNetwork} to use
	 * @param idx        index of the question in the survey (this is *not* the variable index)
	 * @param skill      skill associated with this question
	 * @param difficulty index in the data argument to use as cpt
	 * @param data       all cpt available
	 * @return a {@link Question} that include the required values to be converted in a {@link QuestionStructure}
	 */
	private Question addQuestion(BayesianNetwork bn, int idx, int skill, int difficulty, double[][] data) {
		logger.info("Adding to network question node={} difficulty={} for skill={}", idx, difficulty, skill);
		final int q = bn.addVariable(2);
		final BayesianFactor f = BayesianFactorFactory.factory()
				.domain(bn.getDomain(skill, q))
				.data(data[difficulty])
				.get();

		bn.addParent(q, skill);
		bn.setFactor(q, f);
		return new Question(q, skill, difficulty, "Q" + idx);
	}

	/**
	 * Utility method that simplify adding a new skill with 4 states to the survey.
	 *
	 * @param variable index of the variable in the model
	 * @param name     name of this skill
	 * @return a valid {@link SkillStructure} that can be added to the survey.
	 */
	private SkillStructure addSurveySkill(int variable, String name) {
		logger.info("Adding skill {}", name);
		return new SkillStructure()
				.setName(name)
				.setVariable(variable)
				.setStates(List.of(
						new StateStructure("A1", 0),
						new StateStructure("A2", 1),
						new StateStructure("B1", 2),
						new StateStructure("B2", 3)
				));
	}

}
