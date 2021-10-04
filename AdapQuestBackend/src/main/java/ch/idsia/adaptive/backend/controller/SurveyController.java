package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.dao.*;
import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.persistence.requests.RequestAnswer;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseResult;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.backend.services.SessionException;
import ch.idsia.adaptive.backend.services.SessionService;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import ch.idsia.adaptive.backend.services.commons.SurveyException;
import ch.idsia.adaptive.backend.utils.Convert;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    24.11.2020 16:43
 */
@Controller
@RequestMapping(value = "/survey", consumes = MediaType.ALL_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class SurveyController {
	private static final Logger logger = LogManager.getLogger(SurveyController.class);

	final SessionService sessions;
	final SurveyManagerService manager;

	final SurveyRepository surveys;
	final StatesRepository statuses;
	final QuestionAnswerRepository questionAnswers;
	final QuestionRepository questions;
	final AnswerRepository answers;

	@Autowired
	public SurveyController(
			SessionService sessions,
			SurveyManagerService manager,
			SurveyRepository surveys,
			StatesRepository statuses,
			QuestionRepository questions,
			QuestionAnswerRepository questionAnswers,
			AnswerRepository answers
	) {
		this.sessions = sessions;
		this.manager = manager;
		this.surveys = surveys;
		this.statuses = statuses;
		this.questions = questions;
		this.questionAnswers = questionAnswers;
		this.answers = answers;
	}

	@GetMapping("/codes")
	public ResponseEntity<Collection<String>> getListOfAvailableAccessTokens(HttpServletRequest request) {
		logger.info("Request list of available access tokens from ip={}", request.getRemoteAddr());

		final Collection<String> codes = surveys.findAllAccessCodes();

		return new ResponseEntity<>(codes, HttpStatus.OK);
	}

	/**
	 * Return the current {@link State} of the {@link Survey} for the session related to the given token.
	 *
	 * @param token identifier of the session generate after an init call
	 * @return a {@link State} that describe the last situation of the survey
	 */
	@GetMapping("/state/{token}")
	public ResponseEntity<ResponseState> getLastStateForToken(@PathVariable("token") String token) {
		logger.info("Request status for token={}", token);

		try {
			final Session session = sessions.getSession(token);
			final State state = statuses.findFirstBySessionOrderByCreationDesc(session);

			if (state == null)
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);

			return new ResponseEntity<>(Convert.toResponse(state), HttpStatus.OK);
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
	@GetMapping("/states/{token}")
	public ResponseEntity<List<ResponseState>> getAllStatesForToken(@PathVariable("token") String token) {
		logger.info("Request all states for accessCode={}", token);

		try {
			final Session session = sessions.getSession(token);
			final List<State> states = statuses.findAllBySessionOrderByCreationDesc(session);

			if (states == null)
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);

			final List<ResponseState> res = states.stream().map(Convert::toResponse).collect(Collectors.toList());

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
	@GetMapping("/init/{code}")
	public ResponseEntity<ResponseData> initTest(@PathVariable("code") String accessCode, HttpServletRequest request) {
		logger.info("Request test initialization with accessCode={}", accessCode);

		try {
			final SurveyData data = new SurveyData()
					.setAccessCode(accessCode)
					.setUserAgent(request.getHeader("User-Agent"))
					.setRemoteAddress(request.getRemoteAddr());

			// update data and assign session token
			final Session session = sessions.registerNewSession(data);

			logger.debug("New initialization for accessCode={} received token={}", accessCode, session.getToken());

			// TODO: initialize a timer or timeout for the time-limit achieved when someone abandons the survey

			manager.init(data);

			final State s = manager.getState(data)
					.setSession(session);
			statuses.save(s);

			return new ResponseEntity<>(Convert.toResponse(data), HttpStatus.OK);
		} catch (SessionException e) {
			logger.warn("Request test initialization with an invalid accessCode={} from ip={}", accessCode, request.getRemoteAddr());
			return new ResponseEntity<>(HttpStatus.NOT_FOUND);
		} catch (Exception e) {
			logger.error(e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/answers/{token}")
	public List<Answer> getAnswers(@PathVariable("token") String token) {
		logger.info("Request all answers for token={}", token);

		return answers.findAllBySessionTokenOrderByCreationAsc(token);
	}

	// TODO: getActiveTests?

	/**
	 * Update the adaptive model for the given user based on its answer.
	 *
	 * @param token   unique session token id
	 * @param answer  answer in JSON format
	 * @param request servlet request component
	 * @return 500 if there is an internal error, otherwise 200
	 */
	@PostMapping(value = "/answer/{token}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<SurveyData> checkAnswer(
			@PathVariable("token") String token,
			@RequestBody RequestAnswer answer,
			HttpServletRequest request
	) {
		final Long questionId = answer.question;
		final Long answerId = answer.answer;
		return checkAnswer(token, questionId, new Long[]{answerId}, request);
	}

	/**
	 * Update the adaptive model for the given user based on its answer.
	 *
	 * @param token      unique session token id
	 * @param questionId question the user answer
	 * @param answersId  answers given by the user
	 * @param request    servlet request component
	 * @return 500 if there is an internal error, otherwise 200
	 */
	@PostMapping(value = "/answer/{token}", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity<SurveyData> checkAnswer(
			@PathVariable("token") String token,
			@RequestParam("question") Long questionId,
			@RequestParam("answers") Long[] answersId,
			HttpServletRequest request
	) {
		logger.info("User with token={} gave answers={}", token, Arrays.toString(answersId));

		try {
			final Session session = sessions.getSession(token);
			final SurveyData data = new SurveyData()
					.setFromSession(session)
					.setUserAgent(request.getHeader("User-Agent"))
					.setRemoteAddress(request.getRemoteAddr());

			// check for all parameters
			if (questionId == null)
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

			if (answersId == null)
				return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

			final Question q = questions.findQuestionBySurveyIdAndId(data.getSurveyId(), questionId);

			if (q.getMultipleChoice()) {
				// multiple answers
				final Set<Long> positiveAnswers = new HashSet<>(Arrays.asList(answersId));
				final Set<Integer> positiveVariables = q.getAnswersAvailable().stream()
						.filter(x -> positiveAnswers.contains(x.getId()))
						.map(QuestionAnswer::getVariable)
						.collect(Collectors.toSet());

				for (QuestionAnswer qa : q.getAnswersAvailable()) {
					// if the answer is checked, we want a 1, else we want a 0
					if (positiveVariables.contains(qa.getVariable()) ? qa.getState() == 1 : qa.getState() == 0) {

						logger.info("Multiple choice answer: id={} state={} variable={}", qa.getId(), qa.getState(), qa.getVariable());

						final Answer answer = new Answer()
								.setSession(session)
								.setQuestionAnswer(qa)
								.setIsCorrect(qa.getIsCorrect())
								.setQuestion(qa.getQuestion());

						final boolean b = manager.checkAnswer(data, answer);
						if (b) {
							answers.save(answer);
						}
					}
				}

				sessions.setLastAnswerTime(session);

				final State s = manager.getState(data).setSession(session);
				statuses.save(s);

			} else {
				// single answers
				if (answersId.length != 1)
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

				final long answerId = answersId[0];

				final QuestionAnswer qa = questionAnswers.findByIdAndQuestionId(answerId, questionId);

				if (qa == null)
					return new ResponseEntity<>(HttpStatus.BAD_REQUEST);

				final Answer answer = new Answer()
						.setSession(session)
						.setQuestionAnswer(qa)
						.setIsCorrect(qa.getIsCorrect())
						.setQuestion(qa.getQuestion());

				final boolean b = manager.checkAnswer(data, answer);
				if (b) {
					answers.save(answer);
					sessions.setLastAnswerTime(session);

					final State s = manager.getState(data).setSession(session);
					statuses.save(s);
				}
			}

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
	@GetMapping("/question/{token}")
	public ResponseEntity<ResponseQuestion> nextQuestion(@PathVariable("token") String token, HttpServletRequest request) {
		logger.info("User with token={} request a new question", token);

		Session session = null;
		SurveyData data = null;
		try {
			session = sessions.getSession(token);
			data = new SurveyData()
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

			final Question q = manager.nextQuestion(data);
			return new ResponseEntity<>(Convert.toResponse(q), HttpStatus.OK);
		} catch (SessionException e) {
			logger.error(e);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		} catch (SurveyException e) {
			if (e.getMessage().equals("Finished")) {
				logger.info("User with token={} has ended with a lower info gain", token);
				manager.complete(data);
				sessions.endSurvey(session);
			} else {
				logger.info("User with token={} has no more questions", token);
			}
			return new ResponseEntity<>(HttpStatus.NO_CONTENT);
		}
	}

	@GetMapping("/results/{token}")
	public ResponseEntity<ResponseResult> surveyResults(@PathVariable("token") String token) {
		logger.info("User with token={} request the results", token);
		try {
			final Session session = sessions.getSession(token);
			final State state = statuses.findFirstBySessionOrderByCreationDesc(session);

			if (state == null)
				return new ResponseEntity<>(HttpStatus.NOT_FOUND);

			final ResponseResult r = Convert.toResponse(session, state);

			return new ResponseEntity<>(r, HttpStatus.OK);
		} catch (SessionException e) {
			logger.warn("Session not found for token={}", token);
			return new ResponseEntity<>(HttpStatus.FORBIDDEN);
		}
	}

}
