package ch.idsia.adaptive.backend.services.commons;


import ch.idsia.adaptive.backend.persistence.model.*;
import ch.idsia.adaptive.backend.services.SurveyManagerService;
import ch.idsia.adaptive.backend.services.commons.agents.AgentGeneric;
import ch.idsia.adaptive.backend.services.commons.profiles.Content;
import ch.idsia.adaptive.backend.services.commons.profiles.Profile;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import lombok.Getter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
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

import static org.apache.poi.ss.usermodel.ConditionalFormattingThreshold.RangeType;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    25.11.2021 11:29
 */
public class JobExperiment {
	private static final Logger logger = LoggerFactory.getLogger(JobExperiment.class);

	private final ExecutorService es;
	private final int nThread;

	private final String filename;
	@Getter
	private String resultFilename;

	private final Survey survey;
	private final List<Profile> profiles;

	public JobExperiment(String filename, Path path, Survey survey, Integer nThread) throws IOException {
		this.filename = filename;
		this.survey = survey;
		this.nThread = nThread;

		profiles = readProfiles(path);

		es = Executors.newFixedThreadPool(Math.min(nThread, profiles.size()));
	}

	private List<Profile> readProfiles(Path path) throws IOException {
		logger.info("Parsing for profiles path={}", path.getFileName());

		// reading answers and profiles
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
						if (row.getCell(j) == null || row.getCell(j).toString().isEmpty())
							continue;

						final String profile = row.getCell(j).getStringCellValue();
						if ("SKILLS".equalsIgnoreCase(profile)) {
							iValSkills = j;
						} else {
							profiles.put(profile, new Profile(profile, j));
						}
					}

					continue;
				}

				if (row.getCell(1) == null) {
					break;
				}

				final String n = row.getCell(iValSkills).getStringCellValue();
				for (Profile p : profiles.values()) {
					final int s = Double.valueOf(row.getCell(p.getCol()).getNumericCellValue()).intValue();
					p.add(n, s);
				}
			}

			// answers parsing
			int iValQuestionId = 0, iValAnswerId = 0, iValProfilesStart = 0, q = -1;

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
						if (row.getCell(j) == null || row.getCell(j).toString().isEmpty())
							continue;
						final String profile = row.getCell(j).getStringCellValue();
						profiles.get(profile).setCol(j);
					}

					continue;
				}

				if (row.getCell(iValProfilesStart) == null || row.getCell(iValProfilesStart).toString().isEmpty()) {
					break;
				}

				if (row.getCell(iValQuestionId) != null && !row.getCell(iValQuestionId).toString().isEmpty()) {
					q = Double.valueOf(row.getCell(iValQuestionId).getNumericCellValue()).intValue();
				}
				int a = Double.valueOf(row.getCell(iValAnswerId).getNumericCellValue()).intValue();

				for (Profile p : profiles.values()) {
					p.add("Q" + q, "A" + a, Double.valueOf(row.getCell(p.getCol()).getNumericCellValue()).intValue());
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
					final AgentGeneric<BayesianFactor> agent = SurveyManagerService.getAgentForSurvey(survey, 42L);
					agent.setExecutor(e);

					try {
						Question question;
						State state;
						double avgScore;

						state = agent.getState();

						content.add(profile.getName()); // profile name
						content.add("INIT"); // question
						content.add(""); // answer
						content.add(""); // answer given
						for (String skill : skills) {
							final double d = state.probabilities.get(skill)[1];
							content.add(d); // P(skill)
						}
						avgScore = 0.0;
						for (String skill : skills) {
							final double score = state.score.get(skill);
							avgScore += score / skills.size();
						}
						content.add(avgScore); // H(avg)
						content.add(agent.getObservations().toString()); // observations

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
									content.add(profile.getName()); // profile
									content.add(q); // question
									content.add(a); // answer

									double v;
									if (question.getYesOnly()) {
										v = ans == 0 ? 0 : +1;
									} else {
										v = ans == 0 ? -1 : +1;
									}
									content.add(v); // answer given
									for (double d : profile.getWeights().get(q).get(a)) {
										content.add(v * d); // P(x)
									}
									content.add(""); // H(avg)
								}
							}

							checked.forEach(qa -> agent.check(new Answer(qa)));

							state = agent.getState();

							content.newLine();
							content.add(profile.getName()); // profile
							content.add(q); // question
							content.add(""); // answer
							content.add(""); // answer given

							for (String s : skills) {
								final double d = state.getProbabilities().get(s)[1];
								content.add(d); // P(x)
							}
							avgScore = 0.0;
							for (String skill : skills) {
								final double score = state.score.get(skill);
								avgScore += score / skills.size();
							}
							content.add(avgScore); // H(avg)
							content.add(agent.getObservations().toString());

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
					content.add(profile.getName()); // profile
					content.add("TRUE"); // question
					content.add(""); // answer
					content.add(""); // answer given
					for (String s : skills) {
						content.add(profile.getSkills().get(s)); // P(x)
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

		resultFilename = "results." + filename;
		final Path dst = Paths.get("", "data", "results", resultFilename);

		try (
				FileOutputStream fos = new FileOutputStream(dst.toFile(), false);
				XSSFWorkbook workbook = new XSSFWorkbook()
		) {
			final XSSFSheet results = workbook.createSheet("results");

			int r = 0;
			int l = Content.header(results.createRow(r++), skills);

			for (Content content : contents) {
				r = content.row(results, r);
			}

			// set filters
			results.setAutoFilter(new CellRangeAddress(0, r, 0, 3));

			// style for header row
			final CellStyle hStyle = workbook.createCellStyle();
			final Font font = workbook.createFont();
			font.setBold(true);
			hStyle.setFont(font);
			hStyle.setAlignment(HorizontalAlignment.CENTER);
			hStyle.setBorderBottom(BorderStyle.THIN);

			final CellStyle hStyleSkills = workbook.createCellStyle();
			hStyleSkills.setFont(font);
			hStyleSkills.setRotation((short) 90);
			hStyleSkills.setWrapText(true);
			hStyleSkills.setAlignment(HorizontalAlignment.CENTER);
			hStyleSkills.setBorderBottom(BorderStyle.THIN);

			// style for columns with borders
			final CellStyle cStyleBorder = workbook.createCellStyle();
			cStyleBorder.setAlignment(HorizontalAlignment.CENTER);
			cStyleBorder.setBorderLeft(BorderStyle.THIN);
			cStyleBorder.setBorderRight(BorderStyle.THIN);

			// style for columns without borders
			final CellStyle cStyle = workbook.createCellStyle();
			cStyle.setAlignment(HorizontalAlignment.CENTER);
			cStyle.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

			// style for Havg column
			final CellStyle cStyleHavg = workbook.createCellStyle();
			cStyleHavg.setAlignment(HorizontalAlignment.CENTER);
			cStyleHavg.setBorderLeft(BorderStyle.THIN);
			cStyleHavg.setBorderRight(BorderStyle.THIN);
			cStyleHavg.setAlignment(HorizontalAlignment.CENTER);
			cStyleHavg.setDataFormat(workbook.createDataFormat().getFormat("0.00"));

			// applying styles
			for (Cell cell : results.getRow(0)) {
				if (cell.getAddress().getColumn() > 3 && cell.getAddress().getColumn() < l - 1)
					cell.setCellStyle(hStyleSkills);
				else
					cell.setCellStyle(hStyle);
			}

			final Set<Integer> cols = Set.of(0, 1, 2, 3, l - 1);
			for (int i = 1; i < results.getLastRowNum(); i++) {
				XSSFRow row = results.getRow(i);
				if (row == null)
					row = results.createRow(i);

				for (int c = 0; c < l; c++) {
					XSSFCell cell = row.getCell(c);
					if (cell == null)
						cell = row.createCell(c);
					if (c == l - 1)
						cell.setCellStyle(cStyleHavg);
					else if (cols.contains(c))
						cell.setCellStyle(cStyleBorder);
					else
						cell.setCellStyle(cStyle);
				}
			}

			// conditional formatting skills
			final SheetConditionalFormatting cf1 = results.getSheetConditionalFormatting();
			final ConditionalFormattingRule rule1 = cf1.createConditionalFormattingColorScaleRule();
			final ColorScaleFormatting csf1 = rule1.getColorScaleFormatting();

			csf1.getThresholds()[0].setRangeType(RangeType.MIN);
			csf1.getThresholds()[1].setRangeType(RangeType.PERCENTILE);
			csf1.getThresholds()[1].setValue(0.5);
			csf1.getThresholds()[2].setRangeType(RangeType.MAX);

			((ExtendedColor) csf1.getColors()[0]).setARGBHex("FFF8696B");
			((ExtendedColor) csf1.getColors()[1]).setARGBHex("FFFFEB84");
			((ExtendedColor) csf1.getColors()[2]).setARGBHex("FF63BE7B");

			final CellRangeAddress[] regions1 = {new CellRangeAddress(1, r, 4, l - 2)};
			cf1.addConditionalFormatting(regions1, rule1);

			// conditional formatting Havg
			final SheetConditionalFormatting cf2 = results.getSheetConditionalFormatting();
			final ConditionalFormattingRule rule2 = cf2.createConditionalFormattingColorScaleRule();
			final ColorScaleFormatting csf2 = rule2.getColorScaleFormatting();

			csf2.getThresholds()[0].setRangeType(RangeType.MIN);
			csf2.getThresholds()[1].setRangeType(RangeType.PERCENTILE);
			csf2.getThresholds()[1].setValue(0.5);
			csf2.getThresholds()[2].setRangeType(RangeType.MAX);

			((ExtendedColor) csf2.getColors()[0]).setARGBHex("FF63BE7B");
			((ExtendedColor) csf2.getColors()[1]).setARGBHex("FFFFEB84");
			((ExtendedColor) csf2.getColors()[2]).setARGBHex("FFF8696B");

			final CellRangeAddress[] regions2 = {new CellRangeAddress(1, r, l - 1, l - 1)};
			cf2.addConditionalFormatting(regions2, rule2);

			// auto size columns
			results.setColumnWidth(0, 3112); // 85px
			results.setColumnWidth(1, 1556); // 42px
			results.setColumnWidth(2, 1556); // 42px
			results.setColumnWidth(3, 1556); // 85px
			for (int i = 4; i < l - 1; i++) {
				results.setColumnWidth(3, 2737); // 75px
			}

			// write to disk the results
			workbook.write(fos);
		}

		es.shutdown();
	}

}
