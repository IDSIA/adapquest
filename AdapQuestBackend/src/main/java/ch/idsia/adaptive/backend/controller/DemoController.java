package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.SurveyData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseResult;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    12.01.2021 15:15
 */
@Controller
@ConditionalOnProperty(prefix = "adapquest.controller", name = "demo")
@RequestMapping("/demo")
public class DemoController {
	private static final Logger logger = LoggerFactory.getLogger(DemoController.class);

	@Value("${adapquest.page.title}")
	private String pageTitle = "AdapQuest";

	@Value("${adapquest.exit.url}")
	private String exitUrl = "";

	@Value("${adapquest.exit.text}")
	private String exitText = "";

	final SurveyRepository surveys;
	final SurveyController controller;

	@Autowired
	public DemoController(SurveyRepository surveys, SurveyController controller) {
		this.surveys = surveys;
		this.controller = controller;
	}

	@GetMapping("/")
	public String index(Model model) {
		final Collection<String> codes = surveys.findAllAccessCodes();
		model.addAttribute("codes", new ArrayList<>(codes));
		model.addAttribute("title", pageTitle);
		model.addAttribute("pageTitle", pageTitle);
		model.addAttribute("description", "This demo is not intended to be a final product or usable in a production environment.");
		return "index";
	}

	@GetMapping("/results/{token}")
	public String results(@PathVariable String token, Model model) {
		model.addAttribute("pageTitle", pageTitle);

		final ResponseEntity<ResponseResult> resResult = this.controller.surveyResults(token);
		final Map<Question, List<Answer>> answers = this.controller.getAnswers(token)
				.stream()
				.collect(Collectors.groupingBy(
						Answer::getQuestion,
						Collectors.toList()
				));

		final List<Question> questions = answers.keySet()
				.stream()
				.sorted(Comparator.comparing(
						q -> answers.get(q).get(0).getCreation())
				)
				.collect(Collectors.toList());

		final HttpStatus statusCode = resResult.getStatusCode();

		if (statusCode.is4xxClientError()) {
			model.addAttribute("code", statusCode);
			model.addAttribute("error", "Cannot load results");
			logger.error("Error loading results for token={}, status={}", token, statusCode);
			return "error";
		}

		final ResponseResult r = resResult.getBody();
		model.addAttribute("result", r);
		model.addAttribute("questions", questions);
		model.addAttribute("answers", answers);

		if (!exitUrl.isEmpty() && !exitText.isEmpty()) {
			model.addAttribute("exitButton", true);
			model.addAttribute("exitURL", exitUrl + "?sid=" + token);
			model.addAttribute("exitText", exitText);
		} else {
			model.addAttribute("exitButton", false);
		}

		return "results";
	}

	@GetMapping("/start/{code}")
	public String start(
			@PathVariable(required = false) String code,
			Model model,
			HttpServletRequest request
	) {
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

		return "redirect:/demo/survey/" + rd.token;
	}

	@RequestMapping(value = "/survey/{token}", method = {RequestMethod.GET, RequestMethod.POST})
	public String survey(
			@PathVariable String token,
			@RequestParam(required = false) Long questionId,
			@RequestParam(required = false) Long answerId,
			@RequestParam(value = "checkboxes", required = false) Long[] multipleAnswersId,
			@RequestParam(required = false, defaultValue = "false") Boolean show,
			Model model,
			HttpServletRequest request
	) {
		model.addAttribute("token", token);
		model.addAttribute("show", show);
		model.addAttribute("pageTitle", pageTitle);

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
		final ResponseEntity<ResponseQuestion> resQuestion = controller.nextQuestion(token, request);

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

		final ResponseQuestion q = resQuestion.getBody();

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
