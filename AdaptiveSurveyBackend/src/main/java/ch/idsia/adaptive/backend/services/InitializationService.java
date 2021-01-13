package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    12.01.2021 17:11
 */
@Service
public class InitializationService {
	private static final Logger logger = LogManager.getLogger(InitializationService.class);

	private static final String ACCESS_CODE = "AdaptiveSurvey-Example";

	private final SurveyRepository surveys;


	@Autowired
	public InitializationService(SurveyRepository surveys) {
		this.surveys = surveys;
	}

	public void init() {
		long surveysNum = surveys.count();

		if (surveysNum > 0) {
			logger.info("Data already initialized: {} surveys found(s)", surveysNum);
			return;
		}

		dummySurvey();

		logger.info("Data initialization  with {} survey(s)", surveys.count());
	}

	private void dummySurvey() {
		// this is just a dummy single-skill example, same as in the tests
		BayesianNetwork bn = new BayesianNetwork();
		int A = bn.addVariable(2); // skill:    A               (low, high)
		int L = bn.addVariable(3); // question: low interest    (a, b, c)
		int M = bn.addVariable(2); // question: medium interest (1, 2)
		int H = bn.addVariable(3); // question: high interest   (*, **, ***)

		bn.addParent(L, A);
		bn.addParent(M, A);
		bn.addParent(H, A);

		BayesianFactor[] factors = new BayesianFactor[4];
		factors[A] = new BayesianFactor(bn.getDomain(A));
		factors[L] = new BayesianFactor(bn.getDomain(A, L));
		factors[M] = new BayesianFactor(bn.getDomain(A, M));
		factors[H] = new BayesianFactor(bn.getDomain(A, H));

		factors[A].setData(new double[]{.4, .6});
		factors[L].setData(new double[]{.2, .4, .7, .8, .6, .3});
		factors[M].setData(new double[]{.4, .6, .6, .4});
		factors[H].setData(new double[]{.8, .6, .3, .2, .4, .7});

		bn.setFactors(factors);

		List<String> modelData = new BayesUAIWriter(bn, "").serialize();

		// single skill
		Skill skill = new Skill()
				.setName("A")
				.setVariable(A)
				.setLevels(List.of(
						new SkillLevel("low", 0.0),
						new SkillLevel("high", 1.0)
				));

		// question levels
		QuestionLevel low = new QuestionLevel().setName("Low interest").setVariable(L);
		QuestionLevel medium = new QuestionLevel().setName("Medium interest").setVariable(M);
		QuestionLevel high = new QuestionLevel().setName("High interest").setVariable(H);

		// 3 questions
		Question q1 = new Question()
				.setQuestion("Question 1")
				.setSkill(skill)
				.setLevel(low)
				.addAnswersAvailable(
						new QuestionAnswer().setText("a").setState(0),
						new QuestionAnswer().setText("b").setState(1),
						new QuestionAnswer().setText("c").setState(2)
				);
		Question q2 = new Question()
				.setQuestion("Question 2")
				.setSkill(skill)
				.setLevel(medium)
				.addAnswersAvailable(
						new QuestionAnswer().setText("1").setState(0),
						new QuestionAnswer().setText("2").setState(1)
				);
		Question q3 = new Question()
				.setQuestion("Question 3")
				.setSkill(skill)
				.setLevel(high)
				.addAnswersAvailable(
						new QuestionAnswer().setText("*").setState(0),
						new QuestionAnswer().setText("**").setState(1),
						new QuestionAnswer().setText("***").setState(2)
				);

		// create new survey
		Survey survey = new Survey()
				.setAccessCode(ACCESS_CODE)
				.setDescription("This is just a description")
				.setDuration(3600L)
				.setQuestions(List.of(q1, q2, q3))
				.setSkillOrder(List.of(skill.getName()))
				.setModelData(String.join("\n", modelData))
				.setMixedSkillOrder(false)
				.setIsAdaptive(false);

		surveys.save(survey);
	}

}
