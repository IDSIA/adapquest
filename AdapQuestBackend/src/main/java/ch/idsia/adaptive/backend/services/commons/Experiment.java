package ch.idsia.adaptive.backend.services.commons;


import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.services.commons.agents.AgentPreciseAdaptiveStructural;
import ch.idsia.adaptive.backend.services.commons.profiles.Content;
import ch.idsia.adaptive.backend.services.commons.profiles.Profile;
import ch.idsia.adaptive.backend.services.commons.scoring.precise.ScoringFunctionExpectedEntropy;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    25.11.2021 11:29
 */
public class Experiment {
	private static final Logger logger = LoggerFactory.getLogger(Experiment.class);

	private final ExecutorService es;
	private final int nThread;

	private final String filename;

	private final Survey survey;
	private final List<Profile> profiles;

	public Experiment(String filename, Path path, Survey survey, Integer nThread) throws IOException {
		this.filename = filename;
		this.survey = survey;
		this.nThread = nThread;

		profiles = readProfiles(path);

		es = Executors.newFixedThreadPool(Math.min(nThread, profiles.size()));
	}

	private List<Profile> readProfiles(Path path) throws IOException {
		logger.info("Parsing for profiles path={}", path.getFileName());

		// reading answers and profiles
		final Map<Integer, String> names = new HashMap<>();
		final Map<String, Profile> profiles = new HashMap<>();

		try (FileInputStream fis = new FileInputStream(path.toFile())) {
			final Workbook workbook = new XSSFWorkbook(fis);

			final Sheet sheetProfiles = workbook.getSheet("Profiles");
			final Sheet sheetAnswers = workbook.getSheet("Answers");
			final Sheet sheetQuestions = workbook.getSheet("Questions");

			// profiles parsing
			int iValSkills = 0;
			for (Row row : sheetProfiles) {
				if (row.getRowNum() == 0) {
					for (int j = 0; j < row.getLastCellNum(); j++) {

						final String profile = row.getCell(j).getStringCellValue();
						if ("SKILLS".equalsIgnoreCase(profile)) {
							iValSkills = j;
						} else {
							names.put(j, profile);
							profiles.put(profile, new Profile(profile));
						}
					}

					continue;
				}

				if (row.getCell(1) == null)
					break;

				final String n = row.getCell(iValSkills).getStringCellValue();
				for (int j = iValSkills + 1, k = 0; k < names.size(); j++, k++) {
					final int s = Double.valueOf(row.getCell(j).getNumericCellValue()).intValue();
					profiles.get(names.get(j)).add(n, s);
				}
			}

			// answers parsing
			names.clear();
			int iValQuestionId = 0, iValAnswerId = 0, iValProfilesStart = 0;

			for (Row row : sheetAnswers) {
				if (row.getRowNum() == 0) {
					for (int j = 0; j < row.getLastCellNum(); j++) {
						switch (row.getCell(j).getStringCellValue().toUpperCase()) {
							case "QUESTION_ID":
								iValQuestionId = j;
								break;
							case "ANSWER_ID":
								iValAnswerId = j;
								break;
							case "PROFILES":
								iValProfilesStart = j;
								break;
						}
					}

					continue;
				}

				if (row.getRowNum() == 1) {
					for (int j = iValProfilesStart, k = 0; k < profiles.size(); j++, k++) {
						final String profile = row.getCell(j).getStringCellValue();
						names.put(j, profile);
					}

					continue;
				}

				if (row.getCell(iValProfilesStart) == null || row.getCell(iValProfilesStart).toString().isEmpty()) {
					break;
				}

				final int q = Double.valueOf(row.getCell(iValQuestionId).getNumericCellValue()).intValue();
				final int a = Double.valueOf(row.getCell(iValAnswerId).getNumericCellValue()).intValue();

				for (int j = iValProfilesStart, k = 0; k < names.size(); j++, k++) {
					profiles.get(names.get(j)).add("Q" + q, "A" + a, Double.valueOf(row.getCell(j).getNumericCellValue()).intValue());
				}
			}

			String qid = "";
			int iStartSkills = 0, limit = 0;

			final List<String> skills = new ArrayList<>();
			final Map<String, Map<String, double[]>> weights = new ConcurrentHashMap<>();

			for (Row row : sheetQuestions) {
				final int i = row.getRowNum();
				if (i == 0) {
					// parse header
					for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
						switch (row.getCell(j).getStringCellValue().toUpperCase()) {
							case "QUESTION_ID":
								iValQuestionId = j;
								break;
							case "ANSWER_ID":
								iValAnswerId = j;
						}
					}

					continue;
				}
				if (i == 1) {
					// parse skills
					boolean found = false;
					for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
						if (row.getCell(j) != null && !row.getCell(j).toString().isEmpty()) {
							if (!found) {
								iStartSkills = j;
								found = true;
							}
							skills.add(row.getCell(j).getStringCellValue());
						}
					}
					limit = skills.size();
					continue;
				}

				if (row.getCell(iStartSkills) == null || row.getCell(iStartSkills).toString().isEmpty())
					// skip empty rows or not model-related questions
					continue;

				if (row.getCell(iValQuestionId) != null && !row.getCell(iValQuestionId).toString().isEmpty()) {
					// update questions
					qid = "Q" + Double.valueOf(row.getCell(iValQuestionId).getNumericCellValue()).intValue();
					weights.put(qid, new HashMap<>());
				}

				if (row.getCell(iValAnswerId) == null || row.getCell(iValAnswerId).toString().isEmpty())
					continue;

				final String aid = "A" + Double.valueOf(row.getCell(iValAnswerId).getNumericCellValue()).intValue();
				final double[] values = new double[skills.size()];

				for (int j = iStartSkills, k = 0; k < limit; j++, k++) {
					final Cell c = row.getCell(j);
					values[k] = "KO".equals(c.toString()) ? -1.0 : c.getNumericCellValue();
				}

				weights.get(qid).put(aid, values);
			}

			profiles.values().forEach(p -> p.setWeights(weights));
		}
		return new ArrayList<>(profiles.values());
	}

	public void run() throws InterruptedException, IOException {
		logger.info("Experiment {}", filename);

		final Set<String> skills = profiles.get(0).getSkills().keySet();

		final List<Callable<Content>> tasks = profiles.stream()
				.map(profile -> (Callable<Content>) () -> {
					final Content content = new Content();
					final ExecutorService e = Executors.newFixedThreadPool(nThread);
					final AgentPreciseAdaptiveStructural agent = new AgentPreciseAdaptiveStructural(survey, 42L, new ScoringFunctionExpectedEntropy());
					agent.setExecutor(e);

					try {
						Question question;
						State state;
						double avgScore;

						state = agent.getState();

						content.add("" + profile.getName()); // profile name
						content.add("INIT"); // question
						content.add(""); // answer
						content.add(""); // answer given
						for (String skill : skills) {
							final double d = state.probabilities.get(skill)[1];
							content.add("" + d); // P(skill)
						}
						avgScore = 0.0;
						for (String skill : skills) {
							final double score = state.score.get(skill);
							avgScore += score / skills.size();
						}
						content.add("" + avgScore); // H(avg)
						content.add("" + agent.getObservations()); // observations

						content.newLine();

						while ((question = agent.next()) != null) {
							final String q = question.getName();

							final List<QuestionAnswer> checked = new ArrayList<>();
							for (QuestionAnswer qa : question.getAnswersAvailable()) {
								final String a = qa.getName();
								final int ans = profile.answer(q, a);

								if (ans == qa.getState()) {
									checked.add(qa);
									logger.debug("{} {} {} {}", profile.getName(), q, a, ans);

									content.newLine();
									content.add("" + profile.getName()); // profile
									content.add("" + q); // question
									content.add("" + a); // answer

									double v;
									if (question.getYesOnly()) {
										v = ans == 0 ? 0 : +1;
									} else {
										v = ans == 0 ? -1 : +1;
									}
									content.add("" + v); // answer given
									for (double d : profile.getWeights().get(q).get(a)) {
										content.add("" + (v * d)); // P(x)
									}
									content.add(""); // H(avg)
								}
							}

							checked.forEach(qa -> agent.check(new Answer(qa)));

							state = agent.getState();

							content.newLine();
							content.add("" + profile.getName()); // profile
							content.add("" + q); // question
							content.add(""); // answer
							content.add(""); // answer given

							for (String s : skills) {
								final double d = state.getProbabilities().get(s)[1];
								content.add("" + d); // P(x)
							}
							avgScore = 0.0;
							for (String skill : skills) {
								final double score = state.score.get(skill);
								avgScore += score / skills.size();
							}
							content.add("" + avgScore); // H(avg)
							content.add("" + agent.getObservations());

							content.newLine();
						}

					} catch (Exception ex) {
						final String message = ex.getMessage();
						if (!"Finished".equals(message)) {
							logger.warn("{}", message);
							ex.printStackTrace();
						}
					}

					content.newLine();
					content.add("" + profile.getName()); // profile
					content.add("TRUE"); // question
					content.add(""); // answer
					content.add(""); // answer given
					for (String s : skills) {
						content.add("" + profile.getSkills().get(s)); // P(x)
					}
					content.newLine();

					e.shutdown();

					return content;
				})
				.collect(Collectors.toList());

		final List<Future<Content>> futures = es.invokeAll(tasks);
		final List<Content> contents = new ArrayList<>();
		for (Future<Content> future : futures) {
			try {
				contents.add(future.get());
			} catch (Exception e) {
				logger.error("Could not get future: {}", e.getMessage(), e);
			}
		}

		final Path dst = Paths.get("", "data", "results", "results." + filename);

		try (FileOutputStream fos = new FileOutputStream(dst.toFile(), false)) {
			final XSSFWorkbook workbook = new XSSFWorkbook();
			final XSSFSheet results = workbook.createSheet("results");

			int r = 0;
			int l = Content.header(results.createRow(r++), skills);

			for (Content content : contents) {
				r = content.row(results, r);
			}

			// set filters
			results.setAutoFilter(new CellRangeAddress(0, r, 0, l));

			// style for header row
			final CellStyle hStyle = workbook.createCellStyle();
			final Font font = workbook.createFont();
			font.setBold(true);
			hStyle.setFont(font);
			hStyle.setBorderBottom(BorderStyle.THIN);

			// style for columns
			final CellStyle cStyle = workbook.createCellStyle();
			cStyle.setBorderLeft(BorderStyle.THIN);
			cStyle.setBorderRight(BorderStyle.THIN);

			for (Cell cell : results.getRow(0)) {
				cell.setCellStyle(hStyle);
			}

			final int[] cols = {0, 1, 2, 3, l - 1};
			for (int i = 1; i < results.getLastRowNum(); i++) {
				final XSSFRow row = results.getRow(i);
				for (int c : cols) {
					XSSFCell cell = row.getCell(c);
					if (cell == null)
						cell = row.createCell(c);
					cell.setCellStyle(cStyle);
				}
			}

			// conditional formatting
			final XSSFSheetConditionalFormatting cf = results.getSheetConditionalFormatting();
			final XSSFConditionalFormattingRule rule = cf.createConditionalFormattingColorScaleRule();
			final CellRangeAddress[] regions = {new CellRangeAddress(1, r, 4, l - 2)};
			cf.addConditionalFormatting(regions, rule);

			// write to disk the results
			workbook.write(fos);
		}

		es.shutdown();
	}

}
