package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.SurveyData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    04.11.2021 11:08
 */
@Controller
@ConditionalOnProperty(prefix = "adapquest.controller", name = "assistant")
@RequestMapping("/assistant")
public class AssistantController {
	private static final Logger logger = LoggerFactory.getLogger(AssistantController.class);

	final SurveyRepository surveys;
	final SurveyController controller;

	@Autowired
	public AssistantController(SurveyRepository surveys, SurveyController controller) {
		this.surveys = surveys;
		this.controller = controller;
	}

	@GetMapping("/")
	public String index(Model model) {
		final Collection<String> code = surveys.findAllAccessCodesAdaptive();
		model.addAttribute("codes", new ArrayList<>(code));
		model.addAttribute("title", "AdapQuest Assistant");
		model.addAttribute("description", "The AdapQuest Assistant is intended to guide a human during an interview to another human.");
		return "index";
	}

	@GetMapping("/start/{code}")
	public String start(
			@PathVariable(required = false) String code,
			Model model,
			HttpServletRequest request
	) {
		logger.info("assistant initialization for code={}", code);

		// initialization
		final ResponseEntity<ResponseData> resData = this.controller.initTest(code, request);
		final HttpStatus statusCode = resData.getStatusCode();
		final ResponseData rd = resData.getBody();

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

		return "redirect:/assistant/survey/" + rd.token;
	}

	@RequestMapping(value = "/survey/{token}", method = {RequestMethod.GET, RequestMethod.POST})
	public String survey(
			@PathVariable String token,
			@RequestParam(required = false) Long questionId,
			@RequestParam(required = false) Long answerId,
			@RequestParam(value = "checkboxes", required = false) Long[] multipleAnswersId,
			Model model,
			HttpServletRequest request
	) {
		logger.info("ranking for token={}", token);
		model.addAttribute("token", token);

		// check answer
		if (questionId != null) {
			final Long[] answers;
			if (answerId != null) {
				// we have an answer of a question: check it
				answers = new Long[]{answerId};
			} else {
				// we have an answer to a multiple-choice question
				if (multipleAnswersId == null)
					answers = new Long[0];
				else
					answers = multipleAnswersId;
			}

			final ResponseEntity<SurveyData> resData = controller.checkAnswer(token, questionId, answers, request);
			final HttpStatus statusCode = resData.getStatusCode();

			if (!statusCode.is2xxSuccessful()) {
				// this happens when we have wrong ids for question and answer, but it is a minor error
				model.addAttribute("code", statusCode);
				model.addAttribute("error", "Could not check answer");
				logger.error("Cannot check answer for token={}, questionId={}, answerId={}, statusCode={}", token, questionId, answerId, statusCode);
			}
		}

		// get current state
		final ResponseEntity<ResponseState> resState = controller.getLastStateForToken(token);

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
		final ResponseEntity<List<ResponseQuestion>> resQuestion = controller.rankQuestions(token, request);

		if (!resQuestion.getStatusCode().is2xxSuccessful()) {
			// this happens when something went wrong
			model.addAttribute("code", HttpStatus.INTERNAL_SERVER_ERROR);
			model.addAttribute("error", "Response data empty");
			logger.error("Cannot get next question for token={}, statusCode={}", token, resState.getStatusCode());
			return "error";
		}

		if (resQuestion.getStatusCode() == HttpStatus.NO_CONTENT) {
			// this happens when the survey ends
			return "redirect:/assistant/results/" + token;
		}

		final List<ResponseQuestion> q = resQuestion.getBody();

		if (q == null) {
			model.addAttribute("code", HttpStatus.INTERNAL_SERVER_ERROR);
			model.addAttribute("error", "Next question is empty");
			logger.error("Next question is empty for token={}", token);
			return "error";
		}

		logger.info("rank for token={}, found {} questions: higher score={}, lower score={}", token, q.size(), q.get(0), q.get(q.size() - 1));
		model.addAttribute("questions", q);

		return "assist";
	}
}
