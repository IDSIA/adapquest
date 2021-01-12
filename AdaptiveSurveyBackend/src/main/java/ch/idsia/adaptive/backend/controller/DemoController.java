package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.Collection;

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

	@Autowired
	public DemoController(SurveyRepository surveys) {
		this.surveys = surveys;
	}

	@GetMapping("/")
	public String index(Model model) {
		logger.debug("Request index page");

		Collection<String> codes = surveys.findAllAccessCodes();
		model.addAttribute("codes", new ArrayList<>(codes));

		return "index";
	}

	@GetMapping("/survey")
	public String survey() {
		return "survey";
	}
}
