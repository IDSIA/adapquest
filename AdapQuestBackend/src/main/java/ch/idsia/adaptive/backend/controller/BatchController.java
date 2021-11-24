package ch.idsia.adaptive.backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

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
	public String index() {
		return "batch";
	}

	@PostMapping("/")
	public String consume(@RequestParam("file") MultipartFile file, Model model) {
		logger.info("received new data for batch experiment");
		// TODO: manage errors, make this an ajax-call
		try {
			if (file == null) {
				logger.warn("Null file");
				throw new IOException("File null");
			}
			if (file.isEmpty()) {
				logger.warn("Received empty file");
				throw new IOException("Empty file");
			}
			final String filename = file.getOriginalFilename();
			if (filename == null) {
				logger.warn("Received file without name");
				throw new IOException("Invalid file name");
			}
			final Path dest = Paths.get("", "data", "batch").resolve(Paths.get(filename)).toAbsolutePath();
			try (InputStream is = file.getInputStream()) {
				Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
			}
			model.addAttribute("message", "Added new file: " + filename);
		} catch (IOException e) {
			logger.error("Could not save file to disk", e);
			model.addAttribute("error", "Could not save file to disk: " + e.getMessage());
		}
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
