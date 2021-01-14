package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.dao.AnswerRepository;
import ch.idsia.adaptive.backend.persistence.dao.QuestionAnswerRepository;
import ch.idsia.adaptive.backend.persistence.dao.StatusRepository;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseResult;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.backend.services.SessionException;
import ch.idsia.adaptive.backend.services.SessionService;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import ch.idsia.adaptive.backend.services.commons.SurveyException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    24.11.2020 16:43
 */
@Controller
@RequestMapping("/survey")
public class SurveyController {
	private static final Logger logger = LogManager.getLogger(SurveyController.class);

	final SessionService sessions;
	final SurveyManagerService manager;

	final SurveyRepository surveys;
	final StatusRepository statuses;
	final QuestionAnswerRepository questions;
	final AnswerRepository answers;

	@Autowired
	public SurveyController(
			SessionService sessions,
			SurveyManagerService manager,
			SurveyRepository surveys,
			StatusRepository statuses,
			QuestionAnswerRepository questions,
			AnswerRepository answers
	) {
		this.sessions = sessions;
		this.manager = manager;
		this.surveys = surveys;
		this.statuses = statuses;
		this.questions = questions;
		this.answers = answers;
	}

	@GetMapping("/codes")
	public ResponseEntity<Collection<String>> getListOfAvailableAccessTokens(HttpServletRequest request) {
		logger.info("Request list of available access tokens from ip={}", request.getRemoteAddr());

		Collection<String> codes = surveys.findAllAccessCodes();

		return new ResponseEntity<>(codes, HttpStatus.OK);
	}

	/**
	 * Return the current {@link State} of the {@link Survey} for the session related to the given token.
	 *
	 * @param token identifier of the session generate after an init call
	 * @return a {@link State} that describe the last situation of the survey
	 */
	@GetMapping("/state")
	@ResponseBody
	public ResponseEntity<ResponseState> getLastStateForToken(@RequestParam("token") String token) {
		logger.info("Request status for token={}", token);

		try {
			Session session = sessions.getSession(token);
			State state = statuses.findFirstBySessionOrderByCreationDesc(session);

			if (state == null)
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);

			return new ResponseEntity<>(new ResponseState(state), HttpStatus.OK);
		} catch (SessionException e) {
			logger.warn("Session not found for token={}", token);
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
	}

	/**
	 * Return all teh {@link State}es of the {@link Survey} for the session related to the given token.
	 *
	 * @param token identifier of the session generate after an init call
	 * @return list of {@link ResponseState}
	 */
	@GetMapping("/states")
	@ResponseBody
	public ResponseEntity<List<ResponseState>> getAllStatesForToken(@RequestParam("token") String token) {
		logger.info("Request all states for accessCode={}", token);

		try {
			Session session = sessions.getSession(token);
			List<State> states = statuses.findAllBySessionOrderByCreationDesc(session);

			if (states == null)
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);

			List<ResponseState> res = states.stream().map(ResponseState::new).collect(Collectors.toList());

			return new ResponseEntity<>(res, HttpStatus.OK);
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
	 * @return 403 if there is an internal error or the input accessCode is not valid, otherwise 200 and the token for this session
	 */
	@GetMapping("/init")
	@ResponseBody
	public ResponseEntity<ResponseData> initTest(@RequestParam("accessCode") String accessCode, HttpServletRequest request) {
		logger.info("Request test initialization with accessCode={}", accessCode);

		try {
			SurveyData data = new SurveyData()
					.setAccessCode(accessCode)
					.setUserAgent(request.getHeader("User-Agent"))
					.setRemoteAddress(request.getRemoteAddr());

			// update data and assign session token
			Session session = sessions.registerNewSession(data);

			logger.info("New initialization for accessCode=" + accessCode + " received token=" + session.getToken());

			// TODO: initialize a timer or timeout for the time-limit achieved when someone abandons the survey

			manager.init(data);

			State s = manager.getState(data)
					.setSession(session);
			statuses.save(s);

			return new ResponseEntity<>(new ResponseData(data), HttpStatus.OK);
		} catch (SessionException e) {
			logger.warn("Request test initialization with an invalid accessCode={} from ip={}", accessCode, request.getRemoteAddr());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			logger.error(e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/answers")
	public List<Answer> getAnswers(String accessCode) {
		logger.info("Request all answers for accessCode={}", accessCode);

		return answers.findAllBySessionTokenOrderByCreationAsc(accessCode);
	}

	// TODO: getActiveTests?

	/**
	 * Update the adaptive model for the given user based on its answer.
	 *
	 * @param token      unique session token id
	 * @param questionId question the user answer
	 * @param answerId   answer given by the user
	 * @param request    servlet request component
	 * @return 500 if there is an internal error, otherwise 200
	 */
	@PostMapping("/answer")
	public ResponseEntity<SurveyData> checkAnswer(
			@RequestParam("token") String token,
			@RequestParam("question") Long questionId,
			@RequestParam("answer") Long answerId,
			HttpServletRequest request
	) {
		logger.info("User with token={} gave answer={}", token, answerId);

		try {
			Session session = sessions.getSession(token);
			SurveyData data = new SurveyData()
					.setFromSession(session)
					.setUserAgent(request.getHeader("User-Agent"))
					.setRemoteAddress(request.getRemoteAddr());

			// check for all parameters
			if (questionId == null)
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

			if (answerId == null)
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

			QuestionAnswer qa = questions.findByIdAndQuestionId(answerId, questionId);

			if (qa == null)
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

			Answer answer = new Answer()
					.setSession(session)
					.setQuestionAnswer(qa)
					.setIsCorrect(qa.getIsCorrect())
					.setQuestion(qa.getQuestion());

			answer = answers.save(answer);
			manager.checkAnswer(data, answer);

			sessions.setLastAnswer(session, answer);

			State s = manager.getState(data).setSession(session);

			statuses.save(s);

			return new ResponseEntity<>(HttpStatus.OK);
		} catch (SessionException e) {
			logger.error(e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Request the service to return the complete exercise that the interface will display to the user.
	 *
	 * @param token   unique session token id
	 * @param request servlet request component
	 * @return 500 if there is an internal error, 204 if the survey has ended, otherwise 200 with the data of the
	 * question to pose
	 */
	@GetMapping("/question")
	public ResponseEntity<ResponseQuestion> nextQuestion(@RequestParam("token") String token, HttpServletRequest request) {
		logger.info("User with token={} request a new question", token);

		try {
			Session session = sessions.getSession(token);
			SurveyData data = new SurveyData()
					.setFromSession(session)
					.setUserAgent(request.getHeader("User-Agent"))
					.setRemoteAddress(request.getRemoteAddr());

			// check for end time
			if (sessions.getRemainingTime(data) <= 0) {
				logger.info("User with token={} has ended with no remaining time", token);
				manager.complete(data);
				sessions.endSurvey(session);
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			if (manager.isFinished(data)) {
				logger.info("User with token={} has ended with a finished survey", token);
				manager.complete(data);
				sessions.endSurvey(session);
				return new ResponseEntity<>(HttpStatus.NO_CONTENT);
			}

			Question q = manager.nextQuestion(data);
			return new ResponseEntity<>(new ResponseQuestion(q), HttpStatus.OK);
		} catch (SessionException e) {
			logger.error(e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SurveyException e) {
			logger.error(e);
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		}
	}

	@GetMapping("/results")
	public ResponseEntity<ResponseResult> surveyResults(String token) {
		logger.info("User with token={} request the results", token);
		try {
			Session session = sessions.getSession(token);
			State state = statuses.findFirstBySessionOrderByCreationDesc(session);

			if (state == null)
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);

			ResponseResult r = new ResponseResult(session, state);

			return new ResponseEntity<>(r, HttpStatus.OK);
		} catch (SessionException e) {
			logger.warn("Session not found for token={}", token);
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
	}

}
