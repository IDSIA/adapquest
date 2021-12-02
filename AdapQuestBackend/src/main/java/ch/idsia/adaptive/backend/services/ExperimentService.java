package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.dao.ExperimentRepository;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.model.Experiment;
import ch.idsia.adaptive.backend.persistence.model.QuestionAnswer;
import ch.idsia.adaptive.backend.persistence.model.Skill;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.commons.JobExperiment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    25.11.2021 09:32
 */
@Service
public class ExperimentService {
	private static final Logger logger = LoggerFactory.getLogger(ExperimentService.class);

	@Value("${experiment.parallel.pool.size:-1}")
	private Integer poolSize = -1;

	private final InitializationService initService;
	private final ExperimentRepository experimentRepository;

	@Autowired
	public ExperimentService(InitializationService initService, ExperimentRepository experimentRepository) {
		this.initService = initService;
		this.experimentRepository = experimentRepository;
	}

	@Async
	public void exec(String filename) {
		logger.info("Starting new experiment: filename={}", filename);
		Experiment exp = new Experiment()
				.setStatus("INIT")
				.setName(filename);
		experimentRepository.save(exp);

		try {
			final Path path = Paths.get("", "data", "experiments", filename);
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
			final int nThread = poolSize < 0 ? Runtime.getRuntime().availableProcessors() : poolSize;
			final JobExperiment experiment = new JobExperiment(filename, path, survey, nThread);

			exp.setStatus("RUNNING");
			experimentRepository.save(exp);

			experiment.run();

			exp.setResult(experiment.getResultFilename())
					.setStatus("COMPLETED")
					.setCompletion(LocalDateTime.now())
					.setCompleted(true);
			experimentRepository.save(exp);

		} catch (Exception e) {
			exp.setStatus("FAILED");
			experimentRepository.save(exp);
			logger.error("", e);
		}

		logger.info("Experiment ended.");
	}

}
