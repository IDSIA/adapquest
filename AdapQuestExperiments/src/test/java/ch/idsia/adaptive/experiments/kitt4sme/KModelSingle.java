package ch.idsia.adaptive.experiments.kitt4sme;

import ch.idsia.adaptive.backend.persistence.external.*;
import ch.idsia.adaptive.experiments.models.AbstractAdaptiveModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.factor.bayesian.BayesianNoisyOrFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;

import java.util.*;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    30.09.2021 14:52
 */
public class KModelSingle extends AbstractAdaptiveModel {

	final static double eps = 0.1;
	final static double INHIBITOR_MAX_VALUE = 0.95;
	final static double INHIBITOR_MIN_VALUE = 0.05;

	final BayesianNetwork model = new BayesianNetwork();
	final List<BayesianFactor> factors = new ArrayList<>();

	final List<SkillStructure> skills = new ArrayList<>();
	final List<QuestionStructure> questions = new ArrayList<>();

	final Set<String> skillNames = new HashSet<>();
	final Map<Integer, String> variableNames = new HashMap<>();
	final Map<String, Integer> nameVariables = new HashMap<>();

	public KModelSingle() {
		super("KModelExperiment-SingleBinaryQuestions");
	}

	void addSkill(String name) {
		final int q = model.addVariable(2);
		factors.add(BayesianFactorFactory.factory().domain(model.getDomain(q)).data(new double[]{.5, .5}).get());

		final SkillStructure s = new SkillStructure()
				.setName(name)
				.setVariable(q)
				.setStates(List.of(
						new StateStructure("no", 0),
						new StateStructure("yes", 1)
				));
		skills.add(s);
		skillNames.add(name);
		variableNames.put(q, name);
		nameVariables.put(name, q);
	}

	void addQuestion(List<KBinaryQuestion> bqs) {

		for (KBinaryQuestion bq : bqs) {
			final List<Integer> parents = new ArrayList<>();
			final List<Double> inhibitors = new ArrayList<>();
			final List<String> ko = new ArrayList<>();
			final Set<String> skills = bq.values.keySet();

			bq.values.forEach((k, v) -> {
				if (v > 0) {
					parents.add(nameVariables.get(k));
					inhibitors.add(min(INHIBITOR_MAX_VALUE, max(INHIBITOR_MIN_VALUE, 1.0 - v + eps)));
				} else if (v < 0) {
					ko.add(k);
				}
			});

			// TODO: killer features

			// noisy or
			final int nor = model.addVariable(2);

			final int[] p = parents.stream().mapToInt(x -> x).toArray();
			final double[] i = inhibitors.stream().mapToDouble(x -> x).toArray();

			model.addParents(nor, p);

			parents.add(nor);

			final AnswerStructure neg = new AnswerStructure("no", nor, 0);
			final AnswerStructure pos = new AnswerStructure("yes", nor, 1);
			if (ko.size() > 0) {
				// direct evidence
				final List<Integer> evVars = new ArrayList<>();
				final List<Integer> evStates = new ArrayList<>();
				for (String s : ko) {
					evVars.add(nameVariables.get(s));
					evStates.add(0);
				}
				pos.setDirectEvidence(true)
						.setDirectEvidenceVariables(evVars)
						.setDirectEvidenceStates(evStates);
			}

			// question text
			questions.add(
					new QuestionStructure()
							.setName("Q" + bq.questionId + "." + bq.answerId)
							.setQuestion(bq.binaryQuestionText)
							.setMandatory(bq.mandatory)
							.setMultipleChoice(false)
							.setSkills(skills)
							.setAnswers(List.of(neg, pos))
			);

			// factor
			final int[] vars = parents.stream().mapToInt(x -> x).toArray();

			final BayesianNoisyOrFactor f_nor = BayesianFactorFactory.factory().domain(model.getDomain(vars)).noisyOr(p, i);
			factors.add(f_nor);
		}

	}

	@Override
	public String model() {
		model.setFactors(factors.toArray(BayesianFactor[]::new));
		return String.join("\n", new BayesUAIWriter(model, "").serialize());
	}

	@Override
	public List<SkillStructure> skills() {
		return skills;
	}

	@Override
	public List<QuestionStructure> questions() {
		return questions;
	}

	@Override
	public SurveyStructure survey() {
		return super.survey()
				.setSimple(true)
				.setNoSkill(false);
	}
}
