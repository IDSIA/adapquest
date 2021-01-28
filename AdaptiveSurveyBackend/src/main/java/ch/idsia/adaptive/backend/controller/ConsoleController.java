package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.external.ModelStructure;
import ch.idsia.adaptive.backend.persistence.model.Survey;
import ch.idsia.adaptive.backend.services.InitializationService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;


/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    28.01.2021 09:47
 */
@Controller
@RequestMapping("/console")
public class ConsoleController {
	public static final Logger logger = LogManager.getLogger(ConsoleController.class);

	final InitializationService initService;
	final SurveyRepository surveys;

	@Autowired
	public ConsoleController(InitializationService initService, SurveyRepository surveys) {
		this.initService = initService;
		this.surveys = surveys;
	}

	@PostMapping("/add/survey")
	public ResponseEntity<String> postAddSurvey(
			@RequestHeader("token") String token,
			@RequestParam("survey") ImportStructure survey,
			HttpServletRequest request
	) {
		logger.info("ip={} with token={} requested new survey", request.getRemoteAddr(), token);

		// TODO: check if token is valid

		initService.parseSurvey(survey);

		return new ResponseEntity<>("", HttpStatus.CREATED);
	}

	@PostMapping("/add/model")
	public ResponseEntity<String> postAddModel(
			@RequestHeader("token") String token,
			@RequestParam("accessCode") String code,
			@RequestParam(value = "data", required = false) String data,
			@RequestParam(value = "model", required = false) ModelStructure model,
			HttpServletRequest request
	) {
		logger.info("ip={} with token={} requested new survey", request.getRemoteAddr(), token);

		// TODO: check if token is valid
		final Survey survey = surveys.findByAccessCode(code);

		if (survey == null)
			return new ResponseEntity<>("Access code not found", HttpStatus.NOT_FOUND);

		if (model == null && data == null)
			return new ResponseEntity<>("No model data given", HttpStatus.BAD_REQUEST);

		if (data != null) {
			survey.setModelData(data);
		} else {
			survey.setModelData(InitializationService.parseModelStructure(model, new HashMap<>()));
		}

		surveys.save(survey);

		return new ResponseEntity<>(HttpStatus.CREATED);
	}

}
