package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.dao.*;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.external.ModelStructure;
import ch.idsia.adaptive.backend.persistence.model.Client;
import ch.idsia.adaptive.backend.persistence.model.Session;
import ch.idsia.adaptive.backend.persistence.model.State;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.persistence.requests.RequestClient;
import ch.idsia.adaptive.backend.persistence.requests.RequestCode;
import ch.idsia.adaptive.backend.persistence.requests.RequestKey;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import ch.idsia.adaptive.backend.services.InitializationService;
import ch.idsia.adaptive.backend.utils.Convert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import static ch.idsia.adaptive.backend.security.APIKeyGenerator.generateApiKey;
import static ch.idsia.adaptive.backend.security.APIKeyGenerator.validateApiKey;


/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    28.01.2021 09:47
 * <p>
 * These endpoints are protected with API Keys, see {@link ch.idsia.adaptive.backend.config.SecurityConfig} for moder details.
 */
@Controller
@Transactional
@ConditionalOnProperty(prefix = "adapquest.controller", name = "console")
@RequestMapping(value = "/console", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
public class ConsoleController {
	private static final Logger logger = LoggerFactory.getLogger(ConsoleController.class);

	private final InitializationService initService;

	private final ClientRepository clients;
	private final SurveyRepository surveys;
	private final SessionRepository sessions;
	private final StatesRepository states;
	private final AnswerRepository answers;

	@Value("${magic.api.key}")
	private String magicApiKey;

	@Autowired
	public ConsoleController(InitializationService initService, SurveyRepository surveys, ClientRepository clients, SessionRepository sessions, StatesRepository states, AnswerRepository answers) {
		this.initService = initService;
		this.surveys = surveys;
		this.clients = clients;
		this.sessions = sessions;
		this.states = states;
		this.answers = answers;
	}

	@GetMapping("/key")
	public ResponseEntity<String> getApiKey(
			@RequestHeader("APIKey") String key,
			HttpServletRequest request
	) {
		final String ip = request.getRemoteAddr();
		logger.info("ip={} key={}: testing for key", ip, key);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@PostMapping("/key")
	public ResponseEntity<String> postApiKeyNew(
			@RequestHeader("APIKey") String key,
			@RequestBody RequestClient client,
			HttpServletRequest request
	) {
		final String username = client.username;
		final String email = client.email;
		final String ip = request.getRemoteAddr();
		logger.info("ip={} key={}: requested add new key for username={} email={}", ip, key, username, email);

		if (!magicApiKey.equals(key)) {
			logger.warn("ip={} key={}: invalid request for new key, key is not MAGIC", ip, key);
			return new ResponseEntity<>("Invalid MAGIC key", HttpStatus.FORBIDDEN);
		}

		Client c = clients.findClientByUsernameOrEmail(username, email);

		if (c != null) {
			logger.warn("ip={} key={}: requested key for an existing username={} or email={} (existing id={})", ip, key, username, email, c.getId());
			return new ResponseEntity<>("Username or password already exists", HttpStatus.NOT_ACCEPTABLE);
		}

		try {
			c = new Client().setUsername(username).setEmail(email);
			String apiKey = generateApiKey(magicApiKey, c);
			clients.save(c);

			return new ResponseEntity<>(apiKey, HttpStatus.CREATED);
		} catch (Exception e) {
			logger.warn("ip={} key={}: could not generate key for username={} and email={}", ip, key, username, email);
			return new ResponseEntity<>("Could not generate API key", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping("/key")
	public ResponseEntity<String> deleteApiKey(
			@RequestHeader("APIKey") String key,
			@RequestBody RequestKey keyToDelete,
			HttpServletRequest request
	) {
		final String k = keyToDelete.key;
		final String ip = request.getRemoteAddr();
		logger.info("ip={} key={}: requested deletion of key={}", ip, key, k);

		if (magicApiKey.equals(k)) {
			return new ResponseEntity<>("MAGIC key cannot be deleted", HttpStatus.FORBIDDEN);
		}

		try {
			final Client c = clients.findClientByKey(validateApiKey(magicApiKey, k));
			clients.delete(c);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("ip={} key={}: could not delete key={}", ip, key, k);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping(value = "/model/{code}", consumes = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<String> postAddModel(
			@RequestHeader("APIKey") String key,
			@PathVariable("code") String code,
			@RequestParam(value = "data") String data,
			HttpServletRequest request
	) {
		final String ip = request.getRemoteAddr();
		logger.info("ip={} with key={} requested add model to existing survey code={}", ip, key, code);

		final Survey survey = surveys.findByAccessCode(code);

		if (survey == null) {
			logger.warn("ip={} key={}: access code={} not found", ip, key, code);
			return new ResponseEntity<>("Access code not found", HttpStatus.NOT_FOUND);
		}

		survey.setModelData(data);
		surveys.save(survey);

		logger.warn("ip={} key={}: model updated for survey with code={}", ip, key, survey.getAccessCode());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/model/{code}")
	public ResponseEntity<String> postAddModel(
			@RequestHeader("APIKey") String key,
			@PathVariable("code") String code,
			@RequestParam(value = "model") ModelStructure model,
			HttpServletRequest request
	) {
		final String ip = request.getRemoteAddr();
		logger.info("ip={} with key={} requested add model to existing survey code={}", ip, key, code);

		final Survey survey = surveys.findByAccessCode(code);

		if (survey == null) {
			logger.warn("ip={} key={}: access code={} not found", ip, key, code);
			return new ResponseEntity<>("Access code not found", HttpStatus.NOT_FOUND);
		}

		if (model == null) {
			logger.warn("ip={} key={}: no model data or structure given", ip, key);
			return new ResponseEntity<>("No model data given", HttpStatus.BAD_REQUEST);
		}

		final String data = InitializationService.parseModelStructure(model, new HashMap<>());
		survey.setModelData(data);
		surveys.save(survey);

		logger.warn("ip={} key={}: model updated for survey with code={}", ip, key, survey.getAccessCode());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@PostMapping("/survey")
	public ResponseEntity<String> postAddSurvey(
			@RequestHeader("APIKey") String key,
			@RequestBody ImportStructure surveyStructure,
			HttpServletRequest request
	) {
		final String ip = request.getRemoteAddr();
		logger.info("ip={} key={}: requested add new survey", ip, key);

		final Survey survey = initService.parseSurvey(surveyStructure);

		if (survey == null) {
			logger.error("ip={} key={}: could not save survey to DB from", ip, key);
			return new ResponseEntity<>("Could not save survey to DB", HttpStatus.INTERNAL_SERVER_ERROR);
		}

		logger.warn("ip={} key={}: successfully added new survey with code={}", ip, key, survey.getAccessCode());
		return new ResponseEntity<>("", HttpStatus.CREATED);
	}

	@DeleteMapping("/survey")
	public ResponseEntity<String> removeSurvey(
			@RequestHeader("APIKey") String key,
			@RequestBody RequestCode accessCode,
			HttpServletRequest request
	) {
		final String code = accessCode.accessCode;
		final String ip = request.getRemoteAddr();
		logger.info("ip={} with key={} requested delete of survey by accessCode={}", ip, key, code);

		final Survey survey = surveys.findByAccessCode(code);

		if (survey == null) {
			logger.warn("ip={} key={}: access code={} not found", ip, key, code);
			return new ResponseEntity<>("Access code not found", HttpStatus.NOT_FOUND);
		}

		final List<Session> sessions = this.sessions.findAllByAccessCode(code);

		final Integer answersDeleted = this.answers.deleteBySessionIn(sessions);
		logger.warn("ip={} key={}: deleted {} answers(s) with access code={}", ip, key, answersDeleted, code);

		final Integer statesDeleted = this.states.deleteBySessionIn(sessions);
		logger.warn("ip={} key={}: deleted {} state(s) with access code={}", ip, key, statesDeleted, code);

		final Integer sessionsDeleted = this.sessions.deleteByAccessCode(survey.getAccessCode());
		logger.warn("ip={} key={}: deleted {} session(s) with access code={}", ip, key, sessionsDeleted, code);

		surveys.delete(survey);
		logger.warn("ip={} key={}: survey with access code={} deleted", ip, key, code);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@DeleteMapping("/survey/clean")
	public ResponseEntity<String> cleanSurvey(
			@RequestHeader("APIKey") String key,
			@RequestBody RequestCode accessCode,
			HttpServletRequest request
	) {
		final String code = accessCode.accessCode;
		final String ip = request.getRemoteAddr();
		logger.info("ip={} with key={} requested delete of survey by accessCode={}", ip, key, code);

		final Survey survey = surveys.findByAccessCode(code);

		if (survey == null) {
			logger.warn("ip={} key={}: access code={} not found", ip, key, code);
			return new ResponseEntity<>("Access code not found", HttpStatus.NOT_FOUND);
		}

		final List<Session> sessions = this.sessions.findAllByAccessCode(code);

		final Integer answersDeleted = this.answers.deleteBySessionIn(sessions);
		logger.warn("ip={} key={}: deleted {} answers(s) with access code={}", ip, key, answersDeleted, code);

		final Integer statesDeleted = this.states.deleteBySessionIn(sessions);
		logger.warn("ip={} key={}: deleted {} state(s) with access code={}", ip, key, statesDeleted, code);

		final Integer sessionsDeleted = this.sessions.deleteByAccessCode(survey.getAccessCode());
		logger.warn("ip={} key={}: deleted {} session(s) with access code={}", ip, key, sessionsDeleted, code);

		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping(value = "/survey/{code}", consumes = MediaType.ALL_VALUE)
	public ResponseEntity<String> checkSurvey(
			@RequestHeader("APIKey") String key,
			@PathVariable("code") String code,
			HttpServletRequest request
	) {
		final String ip = request.getRemoteAddr();
		logger.info("ip={} with key={} requested check if it exists a survey with accessCode={}", ip, key, code);

		final Survey survey = surveys.findByAccessCode(code);

		if (survey == null) {
			logger.warn("ip={} key={}: access code={} not found", ip, key, code);
			return new ResponseEntity<>("Access code not found", HttpStatus.NOT_FOUND);
		}

		logger.warn("ip={} key={}: survey with access code={} found", ip, key, code);
		return new ResponseEntity<>(HttpStatus.OK);
	}

	@GetMapping(value = "/survey/states/{code}", consumes = MediaType.ALL_VALUE)
	public ResponseEntity<List<ResponseState>> getAllStatesForSurvey(
			@RequestHeader("APIKey") String key,
			@PathVariable("code") String code,
			HttpServletRequest request
	) {
		final String ip = request.getRemoteAddr();
		logger.info("ip={} with key={} requested all results for survey with accessCode={}", ip, key, code);

		final List<Session> sessions = this.sessions.findAllByAccessCode(code);

		if (sessions.isEmpty()) {
			logger.warn("ip={} key={}: accessCode={} has no sessions", ip, key, code);
			return new ResponseEntity<>(new ArrayList<>(), HttpStatus.NOT_FOUND);
		}

		final List<State> states = this.states.findAllBySessionIn(sessions);
		final List<ResponseState> response = states.stream().map(Convert::toResponse).collect(Collectors.toList());

		logger.warn("ip={} key={}: found {} state(s) with accessCode={}", ip, key, response.size(), code);
		return new ResponseEntity<>(response, HttpStatus.OK);
	}
}
