package ch.idsia.adaptive.backend.controller;

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

	@GetMapping
	public String home(Model model) {
		return "home";
	}

}
