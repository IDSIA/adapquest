package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.model.QuestionAnswer;
import ch.idsia.adaptive.backend.persistence.model.Skill;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.commons.Experiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    25.11.2021 09:32
 */
@Service
@PropertySource("classpath:settings.properties")
public class ExperimentService {
	public static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);

	@Value("${experiment.parallel.pool.size}")
	private Integer poolSize = -1;

	final InitializationService initService;

	@Autowired
	public ExperimentService(InitializationService initService) {
		this.initService = initService;
	}

	@Async
	public void exec(String filename) {
		logger.info("Starting new experiment: filename={}", filename);

		try {
			final Path path = Paths.get("", "data", "batch", filename);
			final ImportStructure structure = initService.parseStructure(path);

			if (structure == null)
				throw new Exception("Invalid structure for filename=" + filename);

			final Survey survey = InitializationService.parseSurveyStructure(structure);

			// this is just a fix to have ids also without a database, for experimenting purposes
			for (Skill skill : survey.getSkills()) {
				skill.setId(skill.getVariable() + 1L);
			}

			survey.getQuestions()
					.forEach(q -> {
						q.setId(Long.parseLong(q.getName().substring(1)));

						for (int i = 0; i < q.getAnswersAvailable().size(); i++) {
							final QuestionAnswer qa = q.getAnswersAvailable().get(i);
							qa.setId(q.getId() * 100 + i + 1L);
						}
					});

			// parse profiles
			final Experiment experiment = new Experiment(path, survey, poolSize);
			experiment.run();
		} catch (Exception e) {
			logger.error("", e);
		}

		logger.info("Experiment ended.");
	}

}
