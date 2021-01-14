package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.ResponseResult;
import ch.idsia.adaptive.backend.persistence.model.SurveyData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    12.01.2021 15:15
 */
// TODO: move this in a dedicated project!
@Controller
@RequestMapping("/demo")
public class DemoController {
	private static final Logger logger = LogManager.getLogger(DemoController.class);

	final SurveyRepository surveys;
	final SurveyController controller;

	@Autowired
	public DemoController(SurveyRepository surveys, SurveyController controller) {
		this.surveys = surveys;
		this.controller = controller;
	}

	@GetMapping("/")
	public String index(Model model) {
		Collection<String> codes = surveys.findAllAccessCodes();
		model.addAttribute("codes", new ArrayList<>(codes));
		return "index";
	}

	@GetMapping("/results/{token}")
	public String results(@PathVariable String token, Model model) {
		ResponseEntity<ResponseResult> resResult = this.controller.surveyResults(token);
		HttpStatus statusCode = resResult.getStatusCode();

		if (statusCode.is4xxClientError()) {
			model.addAttribute("code", statusCode);
			model.addAttribute("error", "Cannot load results");
			logger.error("Error loading results for token={}, status={}", token, statusCode);
			return "error";
		}

		ResponseResult r = resResult.getBody();
		model.addAttribute("question", r);

		return "results";
	}

	@GetMapping("/start/{code}")
	public String start(
			@PathVariable(required = false) String code,
			Model model,
			HttpServletRequest request
	) {
		// initialization
		ResponseEntity<ResponseData> resData = this.controller.initTest(code, request);
		HttpStatus statusCode = resData.getStatusCode();
		ResponseData rd = resData.getBody();

		if (statusCode.is4xxClientError()) {
			// this happens when a wrong access code was used
			model.addAttribute("code", statusCode);
			model.addAttribute("error", "The code " + code + " is not valid");
			logger.error("Invalid access code code={}, status={}", code, statusCode);
			return "error";
		}
		if (rd == null) {
			// this should never happen, fall back to 500
			model.addAttribute("code", HttpStatus.INTERNAL_SERVER_ERROR);
			model.addAttribute("error", "Response data empty");
			logger.error("Empty response data with code code={}, statusCode={}", code, statusCode);
			return "error";
		}

		return "redirect:/demo/survey/" + rd.token;
	}

	@RequestMapping(value = "/survey/{token}", method = {RequestMethod.GET, RequestMethod.POST})
	public String survey(
			@PathVariable String token,
			@RequestParam(required = false) Long questionId,
			@RequestParam(required = false) Long answerId,
			Model model,
			HttpServletRequest request
	) {
		// check answer
		if (questionId != null && answerId != null) {
			// we have an answer of a question: check it
			ResponseEntity<SurveyData> resData = controller.checkAnswer(token, questionId, answerId, request);
			HttpStatus statusCode = resData.getStatusCode();

			if (!statusCode.is2xxSuccessful()) {
				// this happens when we have wrong ids for question and answer, but it is a minor error
				model.addAttribute("code", statusCode);
				model.addAttribute("error", "Could not check answer");
				logger.error("Cannot check answer for token={}, questionId={}, answerId={}, statusCode={}", token, questionId, answerId, statusCode);
			}
		}

		// get current state
		ResponseEntity<ResponseState> resState = controller.getLastStateForToken(token);

		if (!resState.getStatusCode().is2xxSuccessful()) {
			// this is a minor error
			model.addAttribute("code", HttpStatus.INTERNAL_SERVER_ERROR);
			model.addAttribute("error", "Status is empty or missing");
			logger.error("Cannot get last state for token={}, statusCode={}", token, resState.getStatusCode());
		} else {
			ResponseState state = resState.getBody();
			model.addAttribute("state", state);
		}

		// ask for the next question
		ResponseEntity<ResponseQuestion> resQuestion = controller.nextQuestion(token, request);

		if (!resQuestion.getStatusCode().is2xxSuccessful()) {
			// this happens when something went wrong
			model.addAttribute("code", HttpStatus.INTERNAL_SERVER_ERROR);
			model.addAttribute("error", "Response data empty");
			logger.error("Cannot get next question for token={}, statusCode={}", token, resState.getStatusCode());
			return "error";
		}

		if (resQuestion.getStatusCode() == HttpStatus.NO_CONTENT) {
			// this happens when the survey ends
			return "redirect:/demo/results/" + token;
		}

		ResponseQuestion q = resQuestion.getBody();

		if (q == null) {
			model.addAttribute("code", HttpStatus.INTERNAL_SERVER_ERROR);
			model.addAttribute("error", "Next question is empty");
			logger.error("Next question is empty for token={}", token);
			return "error";
		}

		if (q.randomAnswers) {
			Collections.shuffle(q.answers);
		}
		model.addAttribute("question", q);

		return "survey";
	}

}
