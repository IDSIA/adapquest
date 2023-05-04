package ch.idsia.adaptive.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    04.11.2021 14:38
 */
@Controller
@RequestMapping("/")
public class HomeController {
	private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

	@Value("${adapquest.page.title}")
	private String pageTitle = "AdapQuest";

	@Value("${adapquest.controller.assistant}")
	private boolean assistant = true;
	@Value("${adapquest.controller.dashboard}")
	private boolean dashboard = true;
	@Value("${adapquest.controller.demo}")
	private boolean demo = true;
	@Value("${adapquest.controller.experiments}")
	private boolean experiments = true;
	@Value("${adapquest.controller.live}")
	private boolean live = true;

	@GetMapping
	public String home(Model model) {
		logger.debug("assistant flag={}", assistant);
		logger.debug("dashboard flag={}", dashboard);
		logger.debug("demo flag={}", demo);
		logger.debug("experiments flag={}", experiments);
		logger.debug("live flag={}", live);

		if (live)
			return "redirect:/live/";

		model.addAttribute("assistant", assistant);
		model.addAttribute("dashboard", dashboard);
		model.addAttribute("demo", demo);
		model.addAttribute("experiments", experiments);
		model.addAttribute("pageTitle", pageTitle);

		return "home";
	}

}
