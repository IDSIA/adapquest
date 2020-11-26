package ch.idsia.adaptive.backend.controller;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    24.11.2020 14:39
 */
@Controller
@RequestMapping(value = "/dashboard")
public class DashboardController {
	private static final Logger logger = LogManager.getLogger(DashboardController.class);

	@Autowired
	public DashboardController() {
	}

	@GetMapping
	public String dashboard(Model model) {
		logger.info("Request dashboard page.");
		model.addAttribute("page", "dashboard");
		return "dashboard";
	}

}
