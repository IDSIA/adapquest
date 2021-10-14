package ch.idsia.adaptive.experiments.alloy;

import ch.idsia.adaptive.backend.persistence.external.*;
import ch.idsia.adaptive.experiments.models.AbstractAdaptiveModel;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.bif.BIFObject;
import ch.idsia.crema.model.io.bif.BIFParser;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    23.02.2021 13:34
 */
public class AlloyModel extends AbstractAdaptiveModel {

	final BIFObject bif;

	final BayesianNetwork bn;
	final List<SkillStructure> skills = new ArrayList<>();
	final List<QuestionStructure> questions = new ArrayList<>();

	final Set<String> skillNames = new HashSet<>();

	public AlloyModel() throws Exception {
		super("AlloyExperiment");

		bif = BIFParser.read("alloy.bnD.nb.bif");
		bn = bif.network;

		addSkill("target");
		bif.variableName.forEach((name, var) -> {
			if (avoidVariable(name)) {
				this.addQuestion(name);
			}
		});
	}

	boolean avoidVariable(String name) {
		return !skillNames.contains(name);
	}

	void addSkill(String name) {
		final SkillStructure s = new SkillStructure()
				.setName(name)
				.setVariable(bif.variableName.get(name))
				.setStates(List.of(
						new StateStructure("0", 0),
						new StateStructure("1", 1)
				));
		skills.add(s);
		skillNames.add(name);
	}

	void addQuestion(String name) {
		final List<AnswerStructure> answers = bif.variableStates.entrySet().stream()
				.filter(e -> e.getKey().startsWith(name))
				.map(e -> new AnswerStructure(e.getKey().split("\\$")[1], bif.variableName.get(name), e.getValue()))
				.collect(Collectors.toList());

		final QuestionStructure q = new QuestionStructure()
				.setQuestion(name)
				.setName(name)
				.setAnswers(answers);
		questions.add(q);

		questions.get(questions.size() - 1).setSkill("target");
	}

	@Override
	public String model() {
		return String.join("\n", new BayesUAIWriter(bn, "").serialize());
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
		final double e = .05;
		return super.survey()
				.setSimple(false)
				.setQuestionTotalMin(5)
				.setQuestionTotalMax(20)
				.setScoreLowerThreshold(e)
				.setGlobalMeanScoreLowerThreshold(e)
				;
	}
}
