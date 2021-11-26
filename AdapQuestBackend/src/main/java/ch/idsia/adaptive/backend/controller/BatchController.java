package ch.idsia.adaptive.backend.controller;

import ch.idsia.adaptive.backend.services.ExperimentService;
import ch.idsia.adaptive.backend.services.commons.OutFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    23.11.2021 14:23
 */
@Controller
@RequestMapping("/batch")
public class BatchController {
	public static final Logger logger = LoggerFactory.getLogger(BatchController.class);

	private final ExperimentService experimentService;

	final PathMatcher matchXLSX = FileSystems.getDefault().getPathMatcher("glob:**.xlsx");

	@Autowired
	public BatchController(ExperimentService experimentService) {
		this.experimentService = experimentService;
	}

	private List<OutFile> listFiles(String folder) {
		final File file = Paths.get("", "data", folder).toFile();
		final File[] files = file.listFiles();
		if (files == null)
			return new ArrayList<>();
		return Arrays.stream(files)
				.filter(x -> matchXLSX.matches(x.toPath()))
				.sorted(Comparator.comparingLong(File::lastModified))
				.map(OutFile::new)
				.collect(Collectors.toList());
	}

	private String defaultView(Model model) {
		model.addAttribute("experiments", listFiles("batch"));
		model.addAttribute("results", listFiles("results"));
		return "batch";
	}

	@GetMapping("/")
	public String index(Model model) {
		return defaultView(model);
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
			// this start the async experiment
			experimentService.exec(filename);

		} catch (IOException e) {
			logger.error("Could not save file to disk", e);
			model.addAttribute("error", "Could not save file to disk: " + e.getMessage());
		}

		return defaultView(model);
	}

	@GetMapping(value = "/template", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public byte[] downloadTemplate(HttpServletResponse response) throws IOException {
		final String template = "adaptive.questionnaire.template.xlsx";
		final Path path = Paths.get("", "data", "templates", template);
		response.addHeader("Content-Disposition", "attachment; filename=\"" + template + "\"");
		return Files.readAllBytes(path);
	}

	@GetMapping(value = "/experiment/{filename}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public byte[] downloadExperiments(@PathVariable("filename") String filename, HttpServletResponse response) throws IOException {
		logger.info("Request download of experiment {}", filename);
		final Path path = Paths.get("", "data", "batch", filename);
		// TODO: check that the path exists and that the file is only XLSX
		response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		return Files.readAllBytes(path);
	}

	@GetMapping(value = "/result/{filename}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
	@ResponseBody
	public byte[] downloadResults(@PathVariable("filename") String filename, HttpServletResponse response) throws IOException {
		logger.info("Request download of results {}", filename);
		final Path path = Paths.get("", "data", "results", filename);
		// TODO: check that the path exists and that the file is only XLSX
		response.addHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
		return Files.readAllBytes(path);
	}

	@DeleteMapping("/delete/{filename}")
	public String deleteResults(Model model) {
		// TODO: delete the results based on id
		return "error";
	}

}
