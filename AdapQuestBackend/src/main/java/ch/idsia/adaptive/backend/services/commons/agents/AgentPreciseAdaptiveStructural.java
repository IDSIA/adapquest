package ch.idsia.adaptive.backend.services.commons.agents;

import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.services.commons.SurveyException;
import ch.idsia.adaptive.backend.services.commons.inference.precise.InferenceLBP;
import ch.idsia.adaptive.backend.services.commons.scoring.Scoring;
import ch.idsia.crema.factor.bayesian.BayesianDefaultFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.graphical.DAGModel;
import ch.idsia.crema.model.io.uai.BayesUAIParser;
import ch.idsia.crema.utility.ArraysUtil;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    19.10.2021 08:55
 * <br/>
 * This {@link Agent} works by approximating the model with a graph with fewer connections. It supports models that have
 * independent {@link Skill}s and any kind of {@link Question}s.
 */
public class AgentPreciseAdaptiveStructural extends AgentGeneric<BayesianFactor> {
	private static final Logger logger = LoggerFactory.getLogger(AgentPreciseAdaptiveStructural.class);

	protected final DAGModel<BayesianFactor> ref;
	protected Boolean dirty;

	public AgentPreciseAdaptiveStructural(Survey survey, Long seed, Scoring<BayesianFactor> scoringFunction) {
		super(survey, seed, scoringFunction);
		addSkills(survey.getSkills());
		addQuestions(survey.getQuestions());

		inference = new InferenceLBP();

		// reference model: this will be the "source" of variables and factors for the working model
		final List<String> lines = Arrays.stream(survey.getModelData().split("\n")).collect(Collectors.toList());
		ref = new BayesUAIParser(lines).parse();

		// working model
		model = newModelStructure();

		dirty = true;
	}

	/**
	 * @return a new model based on the structure of the {@link #skills} and their parents. <br/>
	 * NOTE: questions should not be a parent!
	 */
	private DAGModel<BayesianFactor> newModelStructure() {
		final BayesianNetwork model = new BayesianNetwork();

		// TODO: what if we have a complex structure?
		skills.forEach(s -> {
			final Integer v = s.getVariable();
			model.addVariable(v, ref.getSize(v));
			model.addParents(v, ref.getParents(v));
			model.setFactor(v, ref.getFactor(v));
		});

		return model;
	}

	/**
	 * Add the question structure to the given model, using {@link #ref} model as reference.
	 *
	 * @param model destination model
	 * @param q     question to add
	 */
	private void addQuestion(DAGModel<BayesianFactor> model, Question q) {
		if (q.getMultipleChoice()) {
			// add multi-choice question
			for (QuestionAnswer qa : q.getAnswersAvailable()) {
				final int v = qa.getVariable();
				model.addVariable(v, ref.getSize(v));
				model.addParents(v, ref.getParents(v));
				model.setFactor(v, ref.getFactor(v));
			}

		} else {
			// add simple question
			final int v = q.getVariable();
			model.addVariable(v, ref.getSize(v));
			model.addParents(v, ref.getParents(v));
			model.setFactor(v, ref.getFactor(v));

		}
	}

	/**
	 * Updates the current {@link #model} with a new one where the structure of the skills is the same (built as in
	 * {@link #newModelStructure()}) but the factors are updated with the results of the inference over the
	 * {@link #observations}, if needed. The {@link #observations} are then cleared.
	 */
	private void updateModel() {
		// compute new factors (if needed)
		final TIntObjectMap<BayesianFactor> map = new TIntObjectHashMap<>();
		final TIntIntMap obs = new TIntIntHashMap();

		for (Skill skill : skills) {
			final Integer s = skill.getVariable();

			if (observations.containsKey(s)) {
				// skill is observed
				obs.put(s, observations.get(s));
				final BayesianDefaultFactor o = BayesianFactorFactory.factory().domain(model.getDomain(s)).set(1.0, obs.get(s)).get();
				map.put(s, o);
			} else if (model.getChildren(s).length > 0) {
				// skill is not observed
				final BayesianFactor f = inference.query(model, observations, s);
				map.put(s, f);
			} else {
				// factor with no questions: skip inference and reuse old value
				map.put(s, model.getFactor(s));
			}
		}

		// assign new factors to a new empty model, discard old one
		model = newModelStructure();
		for (int s : map.keys()) {
			model.setFactor(s, map.get(s));
		}

		// remove child nodes
		observations = obs;
		dirty = false;
	}

	@Override
	protected void register(Question q) {
		super.register(q);
		if (dirty)
			updateModel();
		addQuestion(model, q);
	}

	@Override
	public boolean check(Answer answer) {
		dirty = true;
		return super.check(answer);
	}

	@Override
	public State getState() {
		if (dirty)
			updateModel();

		final State state = new State();

		for (Skill skill : skills) {
			final String s = skill.getName();

			final Integer v = skill.getVariable();
			final BayesianFactor f = model.getFactor(v);

			final double h = scoring.score(f);

			state.getSkills().put(skill.getName(), skill);
			state.getProbabilities().put(s, f.getData());
			state.getScore().put(s, h);

			if (questionsDonePerSkill.containsKey(skill)) {
				final long qdps = questionsDonePerSkill.get(skill).size();
				state.getQuestionsPerSkill().put(s, qdps);

				if (qdps > survey.getQuestionPerSkillMax())
					state.getSkillCompleted().add(s);
			}
		}

		state.setTotalAnswers(questionsDone.size());

		return state;
	}

	@Override
	protected boolean checkStop() {
		if (dirty)
			updateModel();

		if (questions.isEmpty()) {
			// we don't have any more question
			logger.debug("survey finished with no more available questions");
			return true;
		}

		if (questionsDone.size() > survey.getQuestionTotalMax()) {
			// we made too many questions
			logger.debug("survey finished with too many questions (done={}, max={})", questionsDone.size(), survey.getQuestionTotalMax());
			return true;
		}

		if (questionsDone.size() < survey.getQuestionTotalMin()) {
			// we need to make more questions and there are skills that are still valid
			return false;
		}

		// check score levels
		double h = 0;
		for (Skill skill : skills) {
			Integer S = skill.getVariable();

			final BayesianFactor pS = model.getFactor(S);
			final double HS = scoring.score(pS);

			h += HS;
		}

		h /= skills.size();

		if (h < survey.getGlobalMeanScoreLowerThreshold() || h > survey.getGlobalMeanScoreUpperThreshold()) {
			logger.debug("survey finished because the mean global score threshold reached (H={}, lower={}, upper={})",
					h, survey.getGlobalMeanScoreLowerThreshold(), survey.getGlobalMeanScoreUpperThreshold());
			return true;
		}

		return false;
	}

	@Override
	protected Question nextQuestion() throws SurveyException {
		if (dirty)
			updateModel();

		// find the question with the optimal score
		Question nextQuestion = null;
		Double maxIG = -Double.MAX_VALUE;

		final Map<Skill, Double> HSs = new HashMap<>();
		for (Skill skill : skills) {
			final Integer S = skill.getVariable();
			final BayesianFactor PS = this.model.getFactor(S);
			final double HS = scoring.score(PS); // skill score
			HSs.put(skill, HS);
		}

		final List<Callable<Double>> tasks = questions.stream()
				.map(question -> (Callable<Double>) () -> {
					final DAGModel<BayesianFactor> model = this.model.copy();
					addQuestion(model, question);

					final double meanInfoGain;

					if (question.getMultipleChoice())
						meanInfoGain = questionMultipleChoiceScore(model, HSs, question);
					else
						meanInfoGain = questionScore(model, HSs, question);

					logger.debug("question={} with average infoGain={}", question.getName(), meanInfoGain);

					return meanInfoGain;
				})
				.collect(Collectors.toList());

		try {
			final List<Future<Double>> futures = executor.invokeAll(tasks);

			for (int i = 0; i < questions.size(); i++) {
				final Question question = questions.get(i);
				final Double meanInfoGain = futures.get(i).get();

				if (meanInfoGain > maxIG) {
					maxIG = meanInfoGain;
					nextQuestion = question;
					nextQuestion.setScore(maxIG);
				}
			}
		} catch (Exception e) {
			logger.error("Could not perform question search: " + e.getMessage());
		}

		if (questionsDone.size() >= survey.getQuestionTotalMin()) {
			double eps = 1e-9;
			if (maxIG <= eps) {
				logger.info("InfoGain below threshold: maxIG={} eps={}", maxIG, eps);
				finished = true;
				throw new SurveyException("Finished");
			}
		}

		if (nextQuestion != null)
			addQuestion(this.model, nextQuestion);

		return nextQuestion;
	}

	/**
	 * @param HSs      initial entropy scores for each skill
	 * @param question the given {@link Question} to use for inference
	 * @return the mean information gain of the given question
	 */
	private double questionScore(DAGModel<BayesianFactor> model, Map<Skill, Double> HSs, Question question) {
		final Integer Q = question.getVariable();
		final int size = model.getSize(Q);

		final BayesianFactor PQ = inference.query(model, observations, Q);

		double meanInfoGain = 0;
		for (Skill skill : skills) {
			final Integer S = skill.getVariable();
			final Double HS = HSs.get(skill);

			double HSQ = 0;

			for (int i = 0; i < size; i++) {
				final TIntIntMap qi = new TIntIntHashMap(observations);
				question.getQuestionAnswer(Q, i).observe(qi);

				final BayesianFactor PSqi = inference.query(model, qi, S);
				final double Pqi = PQ.getValue(i);
				double HSqi = scoring.score(PSqi);
				HSqi = Double.isNaN(HSqi) ? 0.0 : HSqi;

				HSQ += HSqi * Pqi; // conditional score
			}

			logger.debug("question={} skill={} with HSQ={}", question.getName(), skill.getName(), HSQ);

			meanInfoGain += Math.max(0, HS - HSQ) / skills.size();
		}
		return meanInfoGain;
	}

	/**
	 * @param HSs      initial entropy scores for each skill
	 * @param question the given multiple-choice {@link Question} to use for inference
	 * @return the mean information gain of the given question
	 */
	private double questionMultipleChoiceScore(DAGModel<BayesianFactor> model, Map<Skill, Double> HSs, Question question) {
		final int kj = question.getVariables().size();
		double meanInfoGain = 0;

		final InferenceLBP inference = new InferenceLBP();

		// mean of all possible answers
		for (Integer Q : question.getVariables()) {
			final int size = model.getSize(Q);

			final BayesianFactor PQ = inference.query(model, observations, Q);

			final List<Skill> validSkills = skills.stream()
					.filter(S -> !observations.containsKey(S.getVariable()))
					.filter(S -> ArraysUtil.contains(S.getVariable(), model.getParents(Q)))
					.collect(Collectors.toList());

			final int[] Ss = validSkills.stream().mapToInt(Skill::getVariable).toArray();
			final double[] HSQ = new double[Ss.length];

			for (int i = 0; i < size; i++) {
				final TIntIntMap qi = new TIntIntHashMap(observations);
				final QuestionAnswer answer = question.getQuestionAnswer(Q, i);
				answer.observe(qi);

				final List<BayesianFactor> PSqis = inference.query(model, qi, Ss);
				final double Pqi = PQ.getValue(i);

				for (int j = 0; j < PSqis.size(); j++) {
					BayesianFactor PSqi = PSqis.get(j);
					double HSqi = scoring.score(PSqi);
					HSqi = Double.isNaN(HSqi) ? 0.0 : HSqi;

					HSQ[j] += HSqi * Pqi; // conditional score
				}
			}

			// mean of gain for each skill
			for (int i = 0; i < validSkills.size(); i++) {
				final Skill skill = validSkills.get(i);
				final Double HS = HSs.get(skill);

				final double dHS = HS - HSQ[i];

				logger.debug("question={} skill={} Q={} dHS={}", question.getName(), skill.getName(), Q, dHS);
				meanInfoGain += Math.max(0, dHS) / skills.size();
			}
		}
		return meanInfoGain / kj;
	}
}
