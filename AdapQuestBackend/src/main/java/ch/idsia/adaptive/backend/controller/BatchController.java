package ch.idsia.adaptive.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    23.11.2021 14:23
 */
@Controller
@RequestMapping("/batch")
public class BatchController {
	public static final Logger logger = LoggerFactory.getLogger(BatchController.class);


	@GetMapping("/")
	public String index(Model model) {
		model.addAttribute("title", "AdapQuest Batch Experiments");
		model.addAttribute("description", "The AdapQuest Batch Experiments page is intended to use XLSX templates to perform experiments.");
		return "batch";
	}

	@PostMapping("/")
	public String consume(Model model) {
		model.addAttribute("title", "AdapQuest Batch Experiments");
		model.addAttribute("description", "The AdapQuest Batch Experiments page is intended to use XLSX templates to perform experiments.");
		return "batch";
	}

	@GetMapping(value = "/template", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public byte[] downloadTemplate(HttpServletResponse response) throws IOException {
		final String template = "adaptive.questionnaire.template.xlsx";
		final Path path = Paths.get("", "data", "templates", template);
		response.addHeader("Content-Disposition", "attachment; filename=\"" + template + "\"");
		return Files.readAllBytes(path);
	}

	@GetMapping("/results")
	public String downloadResults(Model model) {
		// TODO: given the id (guid?) of a results, download the tasks
		return "error";
	}

	@GetMapping("/delete")
	public String deleteResults(Model model) {
		// TODO: delete the results based on id
		return "error";
	}

}
