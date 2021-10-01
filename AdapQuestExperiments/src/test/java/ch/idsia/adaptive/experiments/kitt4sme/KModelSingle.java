package ch.idsia.adaptive.experiments.kitt4sme;

import ch.idsia.adaptive.backend.persistence.external.AnswerStructure;
import ch.idsia.adaptive.backend.persistence.external.QuestionStructure;
import ch.idsia.adaptive.backend.persistence.external.SkillStructure;
import ch.idsia.adaptive.backend.persistence.external.StateStructure;
import ch.idsia.adaptive.experiments.models.AbstractAdaptiveModel;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.factor.bayesian.BayesianNoisyOrFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;

import java.util.*;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    30.09.2021 14:52
 */
public class KModelSingle extends AbstractAdaptiveModel {

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
						new StateStructure("0", 0),
						new StateStructure("1", 1)
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

			bq.values.forEach((k, v) -> {
				if (v > 0) {
					parents.add(nameVariables.get(k));
					inhibitors.add(1.0 - v);
				} else if (v < 0) {
					ko.add(k);
				}
			});

			// TODO: killer features

			// noisy or
			final int nor = model.addVariable(2);

			final int[] p = parents.stream().mapToInt(x -> x).toArray();
			final double[] i = inhibitors.stream().mapToDouble(x -> x).toArray();

			parents.add(nor);

			// question text
			questions.add(
					new QuestionStructure()
							.setQuestion(bq.binaryQuestionText)
							.setMandatory(bq.mandatory)
							.setMultipleChoice(false)
							.setVariable(nor)
							.setAnswers(List.of(
									new AnswerStructure().setState(0).setText("no"),
									new AnswerStructure().setState(1).setText("yes")
							))
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
}
