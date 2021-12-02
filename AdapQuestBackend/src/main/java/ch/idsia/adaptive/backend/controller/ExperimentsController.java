package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.persistence.dao.ExperimentRepository;
import ch.idsia.adaptive.backend.persistence.model.Experiment;
import ch.idsia.adaptive.backend.services.ExperimentService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    23.11.2021 14:23
 */
@Controller
@ConditionalOnProperty(prefix = "adapquest.controller", name = "experiments")
@RequestMapping("/experiments")
public class ExperimentsController {
	private static final Logger logger = LoggerFactory.getLogger(ExperimentsController.class);

	private final ExperimentService experimentService;
	private final ExperimentRepository experimentRepository;

	@Autowired
	public ExperimentsController(ExperimentService experimentService, ExperimentRepository experimentRepository) {
		this.experimentService = experimentService;
		this.experimentRepository = experimentRepository;
	}

	private String defaultView(Model model) {
		final List<Experiment> exps = experimentRepository.findAllByOrderByCreationDesc();
		model.addAttribute("experiments", exps);
		return "experiments";
	}

	@GetMapping("/")
	public String index(Model model) {
		return defaultView(model);
	}

	@PostMapping("/")
	public String consume(
			@RequestParam(value = "file", required = false) MultipartFile file,
			@RequestParam(value = "filename", required = false) String filename,
			Model model
	) {
		if (filename != null)
			return delete(filename, model);

		logger.info("received new data for experiments");
		// TODO: make this an ajax-call
		try {
			if (file == null) {
				logger.warn("Null file");
				throw new IOException("File null");
			}
			if (file.isEmpty()) {
				logger.warn("Received empty file");
				throw new IOException("Empty file");
			}

			filename = file.getOriginalFilename();
			if (filename == null) {
				logger.warn("Received file without name");
				throw new IOException("Invalid file name");
			}

			if (!filename.endsWith(".xlsx")) {
				logger.warn("Received file with wrong extension filename={}", filename);
				throw new IOException("Invalid extension.");
			}

			Path dest = Paths.get("", "data", "experiments").resolve(Paths.get(filename)).toAbsolutePath();
			int i = 1;
			while (dest.toFile().exists()) {
				filename = filename.replace(".xlsx", ".v" + i++ + ".xlsx");
				dest = Paths.get("", "data", "experiments").resolve(Paths.get(filename)).toAbsolutePath();
			}
			try (InputStream is = file.getInputStream()) {
				Files.copy(is, dest, StandardCopyOption.REPLACE_EXISTING);
			}

			checkForValidXLSX(dest, filename);

			model.addAttribute("message", "Added new file: " + filename);
			// this start the async experiment
			experimentService.exec(filename);

		} catch (IOException e) {
			logger.error("Could not save file to disk", e);
			model.addAttribute("error", "Could not save file to disk: " + e.getMessage());
		}

		return defaultView(model);
	}

	private String delete(String filename, Model model) {
		logger.info("received request to delete filename={}", filename);

		try {
			final Experiment exp = experimentRepository.findByName(filename);
			if (exp == null)
				throw new Exception("filename not found");

			final Path path1 = Paths.get("", "data", "experiments", filename);
			checkForValidXLSX(path1, filename);
			Files.delete(path1);

			final String result = exp.getResult();
			if (result != null) {
				final Path path2 = Paths.get("", "data", "results", result);
				checkForValidXLSX(path2, filename);
				Files.delete(path2);
			}

			experimentRepository.delete(exp);

			model.addAttribute("message", "Deleted experiment " + filename);
		} catch (Exception e) {
			logger.error("Could not delete file " + filename, e);
			model.addAttribute("error", "Could not delete file " + filename);
		}

		return defaultView(model);
	}

	private void checkForValidXLSX(Path path, String filename) {
		if (!Files.exists(path)) {
			logger.error("Filename={} not found or does not exists", filename);
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found");
		}

		if (!Files.isRegularFile(path)) {
			logger.error("Filename={} is not a regular file", filename);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file name");
		}

		try (FileInputStream fis = new FileInputStream(path.toFile())) {
			new XSSFWorkbook(fis); // this is just to test that the file is a valid format
		} catch (Exception e) {
			logger.error("Filename={} is an invalid file type", filename);
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid file type");
		}
	}

	@GetMapping(value = "/template", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public byte[] downloadTemplate(HttpServletResponse response) throws IOException {
		logger.info("Download template request");
		final String template = "adaptive.questionnaire.template.xlsx";
		final Path path = Paths.get("", "data", "templates", template);

		checkForValidXLSX(path, template);

		response.addHeader("Content-Disposition", "attachment; filename=\"" + template + "\"");
		return Files.readAllBytes(path);
	}

	@GetMapping(value = "/experiment/{filename}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public byte[] downloadExperiments(@PathVariable("filename") String filename, HttpServletResponse response) throws IOException {
		logger.info("Request download of experiment {}", filename);
		final Path path = Paths.get("", "data", "experiments", filename);

		checkForValidXLSX(path, filename);

		response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		return Files.readAllBytes(path);
	}

	@GetMapping(value = "/result/{filename}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public byte[] downloadResults(@PathVariable("filename") String filename, HttpServletResponse response) throws IOException {
		logger.info("Request download of results {}", filename);
		final Path path = Paths.get("", "data", "results", filename);

		checkForValidXLSX(path, filename);

		response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		return Files.readAllBytes(path);
	}

}
