package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.dao.ClientRepository;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.external.ModelStructure;
import ch.idsia.adaptive.backend.persistence.model.Client;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.InitializationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;

import static ch.idsia.adaptive.backend.security.APIKeyGenerator.generateApiKey;
import static ch.idsia.adaptive.backend.security.APIKeyGenerator.validateApiKey;


/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    28.01.2021 09:47
 * <p>
 * These endpoints are protected with API Keys, see {@link ch.idsia.adaptive.backend.config.SecurityConfig} for moder details.
 */
@Controller
@RequestMapping("/console")
public class ConsoleController {
	public static final Logger logger = LogManager.getLogger(ConsoleController.class);

	final ClientRepository clients;

	final InitializationService initService;
	final SurveyRepository surveys;
	@Value("${magic.api.key}")
	private String magicApiKey;

	@Autowired
	public ConsoleController(InitializationService initService, SurveyRepository surveys, ClientRepository clients) {
		this.initService = initService;
		this.surveys = surveys;
		this.clients = clients;
	}

	@PostMapping("/key")
	public ResponseEntity<String> postApiKeyNew(
			@RequestHeader("APIKey") String key,
			@RequestParam("username") String username,
			@RequestParam("email") String email,
			HttpServletRequest request
	) {
		final String ip = request.getRemoteAddr();
		logger.info("ip={} key={}: requested add new key for username={} email={}", ip, key, username, email);

		if (!magicApiKey.equals(key)) {
			logger.info("ip={} key={}: invalid request for new key, key is not MAGIC", ip, key);
			return new ResponseEntity<>("Invalid MAGIC key", HttpStatus.FORBIDDEN);
		}

		Client c = clients.findClientByUsernameOrEmail(username, email);

		if (c != null) {
			logger.info("ip={} key={}: requested key for an existing username={} or email={} (existing id={})", ip, key, username, email, c.getId());
			return new ResponseEntity<>("Username or password already exists", HttpStatus.NOT_ACCEPTABLE);
		}

		try {
			c = new Client().setUsername(username).setEmail(email);
			String apiKey = generateApiKey(magicApiKey, c);
			clients.save(c);

			return new ResponseEntity<>(apiKey, HttpStatus.CREATED);
		} catch (Exception e) {
			logger.info("ip={} key={}: could not generate key for username={} and email={}", ip, key, username, email);
			return new ResponseEntity<>("Could not generate API key", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@DeleteMapping("/key")
	public ResponseEntity<String> deleteApiKey(
			@RequestHeader("APIKey") String key,
			@RequestParam("key") String keyToDelete,
			HttpServletRequest request
	) {
		final String ip = request.getRemoteAddr();
		logger.info("ip={} key={}: requested deletion of key={}", ip, key, keyToDelete);

		if (magicApiKey.equals(keyToDelete)) {
			return new ResponseEntity<>("MAGIC key cannot be deleted", HttpStatus.FORBIDDEN);
		}

		try {
			final Client c = clients.findClientByKey(validateApiKey(magicApiKey, keyToDelete));
			clients.delete(c);
			return new ResponseEntity<>(HttpStatus.OK);
		} catch (Exception e) {
			logger.error("ip={} key={}: could not delete key={}", ip, key, keyToDelete);
			return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@PostMapping("/survey")
	public ResponseEntity<String> postAddSurvey(
			@RequestHeader("APIKey") String key,
			@RequestParam("survey") ImportStructure surveyStructure,
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

	@PostMapping("/model")
	public ResponseEntity<String> postAddModel(
			@RequestHeader("APIKey") String key,
			@RequestParam("accessCode") String code,
			@RequestParam(value = "data", required = false) String data,
			@RequestParam(value = "model", required = false) ModelStructure model,
			HttpServletRequest request
	) {
		final String ip = request.getRemoteAddr();
		logger.info("ip={} with key={} requested add model to existing survey code={}", ip, key, code);

		final Survey survey = surveys.findByAccessCode(code);

		if (survey == null) {
			logger.warn("ip={} key={}: access code={} not found", ip, key, code);
			return new ResponseEntity<>("Access code not found", HttpStatus.NOT_FOUND);
		}

		if (model == null && data == null) {
			logger.warn("ip={} key={}: no model data or structure given", ip, key);
			return new ResponseEntity<>("No model data given", HttpStatus.BAD_REQUEST);
		}

		if (data == null) {
			data = InitializationService.parseModelStructure(model, new HashMap<>());
		}

		survey.setModelData(data);
		surveys.save(survey);

		logger.warn("ip={} key={}: model updated for survey with code={}", ip, key, survey.getAccessCode());
		return new ResponseEntity<>(HttpStatus.CREATED);
	}

	@DeleteMapping("/survey")
	public ResponseEntity<String> deleteSurvey(
			@RequestHeader("APIKey") String key,
			@RequestParam("accessCode") String code,
			HttpServletRequest request
	) {
		final String ip = request.getRemoteAddr();
		logger.info("ip={} with key={} requested delete of survey by accessCode={}", ip, key, code);

		final Survey survey = surveys.findByAccessCode(code);

		if (survey == null) {
			logger.warn("ip={} key={}: access code={} not found", ip, key, code);
			return new ResponseEntity<>("Access code not found", HttpStatus.NOT_FOUND);
		}

		surveys.delete(survey);

		logger.warn("ip={} key={}: survey with access code={} deleted", ip, key, code);
		return new ResponseEntity<>(HttpStatus.OK);
	}
}
