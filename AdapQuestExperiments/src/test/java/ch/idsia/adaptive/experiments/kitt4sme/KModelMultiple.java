package ch.idsia.adaptive.experiments.kitt4sme;

import ch.idsia.adaptive.backend.persistence.external.*;
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
public class KModelMultiple extends AbstractAdaptiveModel {

	final static double eps = 0.01;

	final BayesianNetwork model = new BayesianNetwork();
	final List<BayesianFactor> factors = new ArrayList<>();

	final List<SkillStructure> skills = new ArrayList<>();
	final List<QuestionStructure> questions = new ArrayList<>();

	final Set<String> skillNames = new HashSet<>();
	final Map<Integer, String> variableNames = new HashMap<>();
	final Map<String, Integer> nameVariables = new HashMap<>();

	public KModelMultiple() {
		super("KModelExperiment-MultipleChoice");
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

	void addQuestion(List<KQuestion> qs, Map<String, KAnswer> as, Map<Integer, List<KBinaryQuestion>> bqs) {

		for (KQuestion question : qs) {

			final List<AnswerStructure> answers = new ArrayList<>();

			final List<KBinaryQuestion> bqlist = bqs.get(question.questionId);
			if (bqlist == null)
				continue;

			final Set<String> skills = new HashSet<>();
			for (KBinaryQuestion bq : bqlist) {
				final List<Integer> parents = new ArrayList<>();
				final List<Double> inhibitors = new ArrayList<>();
				final List<String> ko = new ArrayList<>();

				skills.addAll(bq.values.keySet());

				bq.values.forEach((k, v) -> {
					if (v > 0) {
						parents.add(nameVariables.get(k));
						inhibitors.add(1.0 - v + eps);
					} else if (v < 0) {
						ko.add(k);
					}
				});

				// noisy or
				final int nor = model.addVariable(2);

				final int[] p = parents.stream().mapToInt(x -> x).toArray();
				final double[] i = inhibitors.stream().mapToDouble(x -> x).toArray();

				parents.add(nor);
				model.addParents(nor, p);

				final KAnswer a = as.get(bq.questionId + "$" + bq.answerId);

				// displayed in multiple choice
				answers.add(new AnswerStructure()
						.setText(a.text)
						.setVariable(nor)
						.setState(1)
				);
				// hidden state
				answers.add(new AnswerStructure()
						.setText("no")
						.setVariable(nor)
						.setState(0)
				);

				final int[] vars = parents.stream().mapToInt(x -> x).toArray();

				final BayesianNoisyOrFactor f_nor = BayesianFactorFactory.factory().domain(model.getDomain(vars)).noisyOr(p, i);
				factors.add(f_nor);
			}

			questions.add(
					new QuestionStructure()
							.setQuestion(question.questionText)
							.setMandatory(question.mandatory)
							.setMultipleChoice(true)
							.setMultipleSkills(true)
							.setSkills(skills)
							.setAnswers(answers)
			);

			// TODO: killer features
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
				.setSimple(true);
	}
}
