package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.dao.AnswerRepository;
import ch.idsia.adaptive.backend.persistence.dao.StatusRepository;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.backend.services.SessionException;
import ch.idsia.adaptive.backend.services.SessionService;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

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
	final SurveyManagerService modelService;

	final StatusRepository statusRepository;
	final AnswerRepository answerRepository;

	@Autowired
	public SurveyController(
			SessionService sessionService,
			SurveyManagerService modelService,
			StatusRepository statusRepository,
			AnswerRepository answerRepository
	) {
		this.sessionService = sessionService;
		this.modelService = modelService;
		this.statusRepository = statusRepository;
		this.answerRepository = answerRepository;
	}

	/**
	 * Return the current state for each skill of the {@link Survey} for the session related to the given token.
	 *
	 * @param token identifier of the session generate after an init call
	 * @return a map where for each {@link Skill} name there is associated a probability distribution
	 */
	@GetMapping("/state")
	@ResponseBody
	public ResponseEntity<ResponseState> getCurrentState(@RequestParam("token") String token) {
		logger.info("Request status for accessCode={}", token);

		try {
			Session session = sessionService.getSession(token);
			Status status = statusRepository.findBySessionOrderByCreationDesc(session);

			if (status == null)
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);

			return new ResponseEntity<>(new ResponseState(status), HttpStatus.OK);
		} catch (SessionException e) {
			logger.warn("Session not found for token={}", token);
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Initialize the survey with session data
	 *
	 * @param accessCode code to access a survey
	 * @param request    servlet request component
	 * @return the token for this session
	 */
	@GetMapping("/init")
	@ResponseBody
	public ResponseEntity<SurveyData> initTest(@RequestParam("accessCode") String accessCode, HttpServletRequest request) {
		logger.info("Request test initialization with accessCode=" + accessCode);

		try {
			SurveyData data = new SurveyData()
					.setAccessCode(accessCode)
					.setUserAgent(request.getHeader("User-Agent"))
					.setRemoteAddress(request.getRemoteAddr());

			// update data and assign session token
			Session session = sessionService.registerNewSession(data);

			logger.info("New initialization for accessCode=" + accessCode + " received token=" + session.getToken());

			// TODO: initialize a timer or timeout for the time-limit achieved when someone abandons the survey

			modelService.init(data);

			Status s = modelService.getState(data)
					// TODO: add missing parameters: questionsTotal, skillCompleted, ...
					.setSession(session);
			statusRepository.save(s);

			// TODO: insert first session status (empty or trivial data)

			return new ResponseEntity<>(data, HttpStatus.OK);
		} catch (SessionException e) {
			logger.error(e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	// TODO: remove or move to another controller
	@GetMapping("/answers")
	public List<Answer> getAnswers(String accessCode) {
		logger.info("Request all answers for accessCode={}", accessCode);

		return answerRepository.findAllBySessionTokenOrderByCreationAsc(accessCode);
	}

	// TODO: getActiveTests?

	/**
	 * Update the adaptive model for the given user based on its answer.
	 *
	 * @param answer
	 * @param request
	 * @return
	 */
	@PostMapping("/answer")
	public ResponseEntity<SurveyData> checkAnswer(String token, @RequestParam("answer") Answer answer, HttpServletRequest request) {
		try {
			Session session = sessionService.getSession(token);
			SurveyData data = new SurveyData()
					.setFromSession(session)
					.setUserAgent(request.getHeader("User-Agent"))
					.setRemoteAddress(request.getRemoteAddr());

			modelService.checkAnswer(data, answer);

			return new ResponseEntity<>(HttpStatus.OK);
		} catch (SessionException e) {
			logger.error(e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Request the service to return the complete exercise that the interface will display to the user.
	 *
	 * @param token
	 * @param request
	 * @return
	 */
	@GetMapping("/question")
	public ResponseEntity<Question> nextQuestion(@RequestParam("token") String token, HttpServletRequest request) {
		try {
			Session session = sessionService.getSession(token);
			SurveyData data = new SurveyData()
					.setFromSession(session)
					.setUserAgent(request.getHeader("User-Agent"))
					.setRemoteAddress(request.getRemoteAddr());

			// check for end time
			if (sessionService.getRemainingTime(data) <= 0) {
				// TODO: the survey is over
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			if (modelService.isFinished(data)) {
				// TODO: the survey is over
				modelService.complete(data);
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			Question q = modelService.nextQuestion(data);

			return new ResponseEntity<>(q, HttpStatus.OK);
		} catch (SessionException e) {
			logger.error(e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/results")
	public Result surveyResults(String token) {
		// TODO
		throw new NotImplementedException();
	}

}
