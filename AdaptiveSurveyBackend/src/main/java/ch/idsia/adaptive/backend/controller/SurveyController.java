package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.authentication.AuthenticationData;
import ch.idsia.adaptive.backend.persistence.dao.AnswerRepository;
import ch.idsia.adaptive.backend.persistence.dao.StatusRepository;
import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.Result;
import ch.idsia.adaptive.backend.persistence.model.Status;
import ch.idsia.adaptive.backend.services.SessionException;
import ch.idsia.adaptive.backend.services.SessionService;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.Map;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    24.11.2020 16:43
 */
@Controller
@RequestMapping("/survey")
public class SurveyController {
	private static final Logger logger = LogManager.getLogger(SurveyController.class);

	final SessionService sessionService;

	final StatusRepository statusRepository;
	final AnswerRepository answerRepository;

	@Autowired
	public SurveyController(SessionService sessionService, StatusRepository statusRepository, AnswerRepository answerRepository) {
		this.sessionService = sessionService;
		this.statusRepository = statusRepository;
		this.answerRepository = answerRepository;
	}

	@GetMapping("/state")
	public Map<String, double[]> getCurrentState(String accessCode) {
		logger.info("Request status for accessCode={}", accessCode);

		Status status = statusRepository.findBySessionToken(accessCode);
		return status.getState();
	}

	@GetMapping("/answers")
	public List<Answer> getAnswers(String accessCode) {
		logger.info("Request all answers for accessCode={}", accessCode);

		return answerRepository.findAllBySessionTokenOrderByCreationAsc(accessCode);
	}

	// TODO: getActiveTests?

	// TODO: validateAccessCode?

	/**
	 * Initialize the survey with session data
	 *
	 * @param data session data
	 * @return the token for this session
	 */
	@GetMapping("/init")
	public String initTest(AuthenticationData data) {
		try {
			sessionService.registerNewSession(data);

			// TODO: initialize a time for the time-limit?

			return data.getToken();

		} catch (SessionException e) {
			// TODO: error code
			e.printStackTrace();
		}

		// TODO
		throw new NotImplementedException();
	}

	/**
	 * Update the adaptive model for the given user based on its answer.
	 *
	 * @param answer
	 */
	@PostMapping("/answer")
	public void checkAnswer(String token, Answer answer) {
		try {
			sessionService.getSession(token);

		} catch (SessionException e) {
			// TODO: error code
			e.printStackTrace();
		}
		// TODO
		throw new NotImplementedException();
	}

	/**
	 * Request the service to return the complete exercise that the interface will display to the user.
	 *
	 * @param token
	 * @return
	 */
	@GetMapping("/next")
	public Question nextQuestion(String token) {
		try {
			sessionService.getSession(token);

		} catch (SessionException e) {
			// TODO: error code
			e.printStackTrace();
		}
		// TODO
		throw new NotImplementedException();
	}

	@GetMapping("/results")
	public Result surveyResults(String token) {
		// TODO
		throw new NotImplementedException();
	}

}
