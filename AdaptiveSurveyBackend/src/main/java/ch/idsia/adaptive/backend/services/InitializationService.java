package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    12.01.2021 17:11
 */
@Service
public class InitializationService {
	private static final Logger logger = LogManager.getLogger(InitializationService.class);

	private final SurveyRepository surveys;

	private final ObjectMapper om = new ObjectMapper();

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

		readDataFolder();

		logger.info("Data initialization completed with {} survey(s)", surveys.count());
	}

	void readDataFolder() {
		Path cwd = Paths.get("");
		try (Stream<Path> paths = Files.walk(cwd.resolve("data"))) {
			paths
					.filter(Files::isRegularFile)
					.forEach(this::parseSurvey);
		} catch (IOException e) {
			logger.error(e);
		}
	}

	void parseSurvey(Path path) {
		try {
			final ImportStructure structure = om.readValue(path.toFile(), ImportStructure.class);

			// build model
			final BayesianNetwork bn = new BayesianNetwork();
			final Map<String, Integer> v = structure.model.stream()
					.collect(Collectors.toMap(
							// collect map of variables
							s -> s.name,
							k -> bn.addVariable(k.states)
					));
			final BayesianFactor[] factors = structure.model.stream()
					.peek(
							// set parents
							s -> s.parents.forEach(
									p -> bn.addParent(v.get(s.name), v.get(p))
							)
					)
					.map(s -> {
						// get domain from variables
						int[] x = new int[s.parents.size() + 1];
						x[0] = v.get(s.name);
						for (int i = 1; i < x.length; i++)
							x[i] = v.get(s.parents.get(i - 1));

						// build factor
						final BayesianFactor bf = new BayesianFactor((bn.getDomain(x)));
						bf.setData(s.data);
						return bf;
					})
					.toArray(BayesianFactor[]::new);
			bn.setFactors(factors);

			List<String> modelData = new BayesUAIWriter(bn, "").serialize();

			// build skills
			final Map<String, Skill> skills = structure.skills.stream()
					.map(s -> new Skill()
							.setName(s.name)
							.setVariable(v.get(s.name))
							.setStates(s.states.stream()
									.map(l -> new SkillLevel(l.name, l.value))
									.collect(Collectors.toList())
							)
					)
					.collect(Collectors.toMap(Skill::getName, x -> x));

			// build questions
			final List<Question> questions = structure.questions.stream()
					.map(q -> new Question()
							.setQuestion(q.question)
							.setExplanation(q.explanation)
							.setSkill(skills.get(q.skill))
							.setLevel(q.name)
							.setVariable(v.get(q.variable))
							.setWeight(q.weight)
							.setIsExample(q.example)
							.setRandomAnswers(q.randomAnswers)
							.addAnswersAvailable(q.answers.stream()
									.map(a -> new QuestionAnswer()
											.setText(a.text)
											.setState(a.state)
											.setIsCorrect(a.correct)
									)
									.toArray(QuestionAnswer[]::new)
							)
					).collect(Collectors.toList());

			// build survey
			Survey survey = new Survey()
					.setLanguage(structure.survey.language)
					.setAccessCode(structure.survey.accessCode)
					.setDescription(structure.survey.description)
					.setDuration(structure.survey.duration)
					.setQuestions(questions)
					.setSkillOrder(structure.survey.skillOrder)
					.setModelData(String.join("\n", modelData))
					.setMixedSkillOrder(structure.survey.mixedSkillOrder)
					.setIsAdaptive(structure.survey.adaptive)
					.setQuestionsAreRandom(structure.survey.randomQuestions)
					.setQuestionPerSkillMin(structure.survey.questionPerSkillMin)
					.setQuestionPerSkillMax(structure.survey.questionPerSkillMax)
					.setEntropyUpperThreshold(structure.survey.entropyUpperThreshold)
					.setEntropyLowerThreshold(structure.survey.entropyLowerThreshold)
					.setQuestionValidityCheckMin(structure.survey.questionValidityCheckMin)
					.setQuestionTotalMin(structure.survey.questionTotalMin)
					.setQuestionTotalMax(structure.survey.questionTotalMax);

			questions.forEach(q -> q.setSurvey(survey));

			// save survey
			surveys.save(survey);

		} catch (IOException e) {
			logger.error(e);
		}
	}

}
