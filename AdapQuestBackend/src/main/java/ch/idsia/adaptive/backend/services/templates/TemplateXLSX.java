package ch.idsia.adaptive.backend.services.templates;

import ch.idsia.adaptive.backend.persistence.external.*;
import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.bayesian.BayesianFactorFactory;
import ch.idsia.crema.factor.bayesian.BayesianNoisyOrFactor;
import ch.idsia.crema.model.graphical.BayesianNetwork;
import ch.idsia.crema.model.io.uai.BayesUAIWriter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

import static ch.idsia.adaptive.backend.services.templates.Settings.*;
import static java.lang.Math.max;
import static java.lang.Math.min;


/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    03.11.2021 08:45
 */
public class TemplateXLSX {
	private static final Logger logger = LoggerFactory.getLogger(TemplateJSON.class);

	private final BayesianNetwork model = new BayesianNetwork();
	private final List<BayesianFactor> factors = new ArrayList<>();

	private final List<SkillStructure> skills = new ArrayList<>();
	private final List<QuestionStructure> questions = new ArrayList<>();

	private final Map<String, Integer> nameVariables = new HashMap<>();

	private double eps = 0.1;
	private double INHIBITOR_MAX_VALUE = 0.95;
	private double INHIBITOR_MIN_VALUE = 0.05;

	private final SurveyStructure survey = new SurveyStructure();

	private TemplateXLSX() {

	}

	private void addSkill(String name) {
		final int q = model.addVariable(2);
		factors.add(BayesianFactorFactory.factory().domain(model.getDomain(q)).data(new double[]{.5, .5}).get());

		final SkillStructure s = new SkillStructure()
				.setName(name)
				.setVariable(q)
				.setStates(List.of(
						new StateStructure("no", 0),
						new StateStructure("yes", 1)
				));
		skills.add(s);
		nameVariables.put(name, q);
	}

	private void setSettings(Map<String, Cell> settings) {
		final String K_EPS = "EPS";
		final String K_INHIBITOR_MAX_VALUE = "INHIBITOR_MAX_VALUE";
		final String K_INHIBITOR_MIN_VALUE = "INHIBITOR_MIN_VALUE";

		if (settings.containsKey(K_EPS))
			eps = settings.get(K_EPS).getNumericCellValue();
		if (settings.containsKey(K_INHIBITOR_MAX_VALUE))
			INHIBITOR_MAX_VALUE = settings.get(K_INHIBITOR_MAX_VALUE).getNumericCellValue();
		if (settings.containsKey(K_INHIBITOR_MIN_VALUE))
			INHIBITOR_MIN_VALUE = settings.get(K_INHIBITOR_MIN_VALUE).getNumericCellValue();

		if (settings.containsKey(ACCESS_CODE))
			survey.setAccessCode(settings.get(ACCESS_CODE).toString());

		if (settings.containsKey(ACCESS_CODE))
			survey.setAccessCode(settings.get(ACCESS_CODE).toString());
		if (settings.containsKey(LANGUAGE))
			survey.setLanguage(settings.get(LANGUAGE).toString());
		if (settings.containsKey(SURVEY_DESCRIPTION))
			survey.setDescription(settings.get(SURVEY_DESCRIPTION).toString());
		if (settings.containsKey(DURATION))
			survey.setDuration((long) settings.get(DURATION).getNumericCellValue());
		if (settings.containsKey(MIXED_SKILL_ORDER))
			survey.setSkillOrder(List.of(settings.get(MIXED_SKILL_ORDER).getStringCellValue().split(",")));
		if (settings.containsKey(ADAPTIVE))
			survey.setAdaptive(settings.get(ADAPTIVE).getBooleanCellValue());
		if (settings.containsKey(SIMPLE))
			survey.setSimple(settings.get(SIMPLE).getBooleanCellValue());
		if (settings.containsKey(STRUCTURAL))
			survey.setStructural(settings.get(STRUCTURAL).getBooleanCellValue());
		if (settings.containsKey(NO_SKILL))
			survey.setNoSkill(settings.get(NO_SKILL).getBooleanCellValue());
		if (settings.containsKey(RANDOM_QUESTIONS))
			survey.setRandomQuestions(settings.get(RANDOM_QUESTIONS).getBooleanCellValue());
		if (settings.containsKey(QUESTION_PER_SKILL_MIN))
			survey.setQuestionPerSkillMin((int) settings.get(QUESTION_PER_SKILL_MIN).getNumericCellValue());
		if (settings.containsKey(QUESTION_PER_SKILL_MAX))
			survey.setQuestionPerSkillMax((int) settings.get(QUESTION_PER_SKILL_MAX).getNumericCellValue());
		if (settings.containsKey(QUESTION_TOTAL_MIN))
			survey.setQuestionTotalMin((int) settings.get(QUESTION_TOTAL_MIN).getNumericCellValue());
		if (settings.containsKey(QUESTION_TOTAL_MAX))
			survey.setQuestionTotalMax((int) settings.get(QUESTION_TOTAL_MAX).getNumericCellValue());
		if (settings.containsKey(SCORE_LOWER_THRESHOLD))
			survey.setScoreLowerThreshold(settings.get(SCORE_LOWER_THRESHOLD).getNumericCellValue());
		if (settings.containsKey(SCORE_UPPER_THRESHOLD))
			survey.setScoreUpperThreshold(settings.get(SCORE_UPPER_THRESHOLD).getNumericCellValue());
		if (settings.containsKey(GLOBAL_MEAN_SCORE_UPPER_THRESHOLD))
			survey.setGlobalMeanScoreUpperThreshold(settings.get(GLOBAL_MEAN_SCORE_UPPER_THRESHOLD).getNumericCellValue());
		if (settings.containsKey(GLOBAL_MEAN_SCORE_LOWER_THRESHOLD))
			survey.setGlobalMeanScoreLowerThreshold(settings.get(GLOBAL_MEAN_SCORE_LOWER_THRESHOLD).getNumericCellValue());
	}

	private void addQuestions(List<TQuestion> qs, Map<String, TAnswer> as, Map<Integer, List<TBinaryQuestion>> bqs) {
		for (TQuestion question : qs) {

			final List<AnswerStructure> answers = new ArrayList<>();

			final List<TBinaryQuestion> bqlist = bqs.get(question.questionId);
			if (bqlist == null)
				continue;

			final Set<String> skills = new HashSet<>();
			for (TBinaryQuestion bq : bqlist) {
				final List<Integer> parents = new ArrayList<>();
				final List<Double> inhibitors = new ArrayList<>();
				final List<String> ko = new ArrayList<>();

				skills.addAll(bq.values.keySet());

				bq.values.forEach((k, v) -> {
					if (v > 0) {
						final double inh = min(INHIBITOR_MAX_VALUE, max(INHIBITOR_MIN_VALUE, 1.0 - v + eps));
						parents.add(nameVariables.get(k));
						inhibitors.add(inh);
					} else if (v < 0) {
						ko.add(k);
					}
				});

				// noisy or
				final int nor = model.addVariable(2);

				final int[] p = parents.stream().mapToInt(x -> x).toArray();
				final double[] i = inhibitors.stream().mapToDouble(x -> x).toArray();

				parents.add(nor);
				model.addParents(nor, p);

				final TAnswer a = as.get(bq.questionId + "$" + bq.answerId);

				// displayed in multiple choice
				final AnswerStructure neg = new AnswerStructure("no", nor, 0).setName("A" + bq.answerId);
				final AnswerStructure pos = new AnswerStructure(a.text, nor, 1).setName("A" + bq.answerId);

				if (ko.size() > 0) {
					// direct evidence
					final List<Integer> evVars = new ArrayList<>();
					final List<Integer> evStates = new ArrayList<>();
					for (String s : ko) {
						evVars.add(nameVariables.get(s));
						evStates.add(0);
					}
					pos.setDirectEvidence(true)
							.setDirectEvidenceVariables(evVars)
							.setDirectEvidenceStates(evStates);
				}

				answers.add(neg);
				answers.add(pos);

				final int[] vars = parents.stream().mapToInt(x -> x).toArray();

				final BayesianNoisyOrFactor f_nor = BayesianFactorFactory.factory().domain(model.getDomain(vars)).noisyOr(p, i);
				factors.add(f_nor);
			}

			questions.add(
					new QuestionStructure()
							.setName("Q" + question.questionId)
							.setQuestion(question.questionText)
							.setMandatory(question.mandatory)
							.setMultipleChoice(true)
							.setMultipleSkills(true)
							.setSkills(skills)
							.setAnswers(answers)
			);
		}
	}

	private String model() {
		model.setFactors(factors.toArray(BayesianFactor[]::new));
		return String.join("\n", new BayesUAIWriter(model, "").serialize());
	}

	private ImportStructure structure() {
		return new ImportStructure()
				.setSurvey(survey)
				.setSkills(skills)
				.setQuestions(questions)
				.setModelData(model());
	}

	public static ImportStructure parse(Path path) throws IOException {

		logger.info("Reading template XLSX file={}", path.toFile());

		final Map<String, Cell> settings = new HashMap<>();
		final List<TQuestion> questions = new ArrayList<>();
		final List<TAnswer> answers = new ArrayList<>();
		final List<String> variables = new ArrayList<>();
		final List<TBinaryQuestion> binaryQuestions = new ArrayList<>();

		try (final Workbook workbook = new XSSFWorkbook(new FileInputStream(path.toFile()))) {
			final Sheet sheetSettings = workbook.getSheet("Settings");
			final Sheet sheetQuestions = workbook.getSheet("Questions");
			final Sheet sheetAnswers = workbook.getSheet("Answers");
			final Sheet sheetBinaryQuestions = workbook.getSheet("Binary questions");

			// reading settings
			int iKey = 0, iValue = 0;
			for (Row row : sheetSettings) {
				if (row.getRowNum() == 0) {
					for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
						switch (row.getCell(i).getStringCellValue()) {
							case "Key":
								iKey = i;
								break;
							case "Value":
								iValue = i;
								break;
						}
					}

					continue;
				}

				if (row.getCell(iKey) != null && !row.getCell(iKey).toString().isEmpty() &&
						row.getCell(iValue) != null && !row.getCell(iValue).toString().isEmpty())
					settings.put(row.getCell(iKey).toString().toUpperCase(), row.getCell(iValue));
			}

			// reading questions
			int iQuestionId = 0, iQuestionMandatory = 1, iQuestionExclusivity = 3, iQuestionText = 4;
			for (Row row : sheetQuestions) {
				if (row.getRowNum() == 0) {
					// parse header
					for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
						switch (row.getCell(i).getStringCellValue().toUpperCase()) {
							case "QUESTION_ID":
								iQuestionId = i;
								break;
							case "MANDATORY":
								iQuestionMandatory = i;
								break;
							case "EXCLUSIVITY":
								iQuestionExclusivity = i;
								break;
							case "QUESTION_TEXT":
								iQuestionText = i;
								break;
						}
					}

					continue;
				}

				if (row.getCell(iQuestionId) == null || row.getCell(iQuestionId).toString().isEmpty())
					continue;

				final int qid = Double.valueOf(row.getCell(iQuestionId).getNumericCellValue()).intValue();
				final boolean man = Double.valueOf(row.getCell(iQuestionMandatory).getNumericCellValue()).intValue() == 1;
				final boolean exc = Double.valueOf(row.getCell(iQuestionExclusivity).getNumericCellValue()).intValue() == 1;
				final String text = row.getCell(iQuestionText).getStringCellValue();

				questions.add(new TQuestion(qid, man, exc, text));
			}

			// reading answers
			int iAnswerQuestionId = 0, iAnswerStart = 2, limit = 0;
			for (Row row : sheetAnswers) {
				if (row.getRowNum() == 0) {
					limit = row.getLastCellNum();
					for (int i = row.getFirstCellNum(); i < row.getLastCellNum(); i++) {
						switch (row.getCell(i).getStringCellValue().toUpperCase()) {
							case "QUESTION_ID":
								iAnswerQuestionId = i;
								break;
							case "ID_ANS_01":
								iAnswerStart = i;
								break;
						}
					}
					continue;
				}

				if (row.getCell(iAnswerQuestionId) == null || row.getCell(iAnswerQuestionId).toString().isEmpty())
					continue;

				final int qid = Double.valueOf(row.getCell(iAnswerQuestionId).getNumericCellValue()).intValue();

				for (int j = iAnswerStart; j < limit; j += 2) {
					final Cell cid = row.getCell(j);
					final Cell txt = row.getCell(j + 1);
					if (row.getCell(j) == null
							|| row.getCell(j + 1) == null
							|| cid.toString().isEmpty()
							|| txt.toString().isEmpty()
					)
						continue;

					final int aid = Double.valueOf(row.getCell(j).getNumericCellValue()).intValue();
					final String text = row.getCell(j + 1).getStringCellValue();

					answers.add(new TAnswer(qid, aid, text));
				}
			}

			// reading model elicitation
			int bqid = 0, iValQuestionId = 0, iValQuestionText = 0, iValMandatory = 0, iValAnswerId = 0, iValAnswerText = 0, iStartSkills = 0;
			limit = 0;

			int qid = -1;
			int man = -1;
			String qText = "";

			for (Row row : sheetBinaryQuestions) {
				final int i = row.getRowNum();
				if (i == 0) {
					// parse header
					for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
						switch (row.getCell(j).getStringCellValue().toUpperCase()) {
							case "Q_ID":
								iValQuestionId = j;
								break;
							case "M":
								iValMandatory = j;
								break;
							case "QUESTIONS_TEXT":
								iValQuestionText = j;
								break;
							case "ID_ANS":
								iValAnswerId = j;
								break;
							case "BINARY_QUESTIONS_TEXT":
								iValAnswerText = j;
								break;
						}
					}

					continue;
				}
				if (i == 1) {
					boolean found = false;
					for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
						if (row.getCell(j) != null && !row.getCell(j).toString().isEmpty()) {
							if (!found) {
								iStartSkills = j;
								found = true;
							}
							variables.add(row.getCell(j).getStringCellValue());
						}
					}
					limit = variables.size();
					continue;
				}

				if (row.getCell(iValQuestionText) == null || row.getCell(iValQuestionText).toString().isEmpty())
					// skip empty rows or not model-related questions
					continue;

				if (row.getCell(iValQuestionId).toString() != null && !row.getCell(iValQuestionId).toString().isEmpty()) {
					// update questions
					qid = Double.valueOf(row.getCell(iValQuestionId).getNumericCellValue()).intValue();
					man = Double.valueOf(row.getCell(iValMandatory).getNumericCellValue()).intValue();
					qText = row.getCell(iValQuestionText).getStringCellValue();
				}

				final int aid = Double.valueOf(row.getCell(iValAnswerId).getNumericCellValue()).intValue();
				final String bqText = row.getCell(iValAnswerText).getStringCellValue();

				if (man < 0)
					continue;

				bqid++;

				final TBinaryQuestion bq = new TBinaryQuestion(qid, aid, bqid, man == 1, qText, bqText);
				binaryQuestions.add(bq);

				for (int j = iStartSkills, k = 0; k < limit; j++, k++) {
					final Cell c = row.getCell(j);
					final double v = c.toString().equals("KO") ? -1.0 : c.getNumericCellValue();
					bq.values.put(variables.get(k), v);
				}
			}
		}

		final Map<String, TAnswer> ansMap = answers.stream().collect(
				Collectors.toMap(k -> k.questionId + "$" + k.answerId, a -> a, (e1, e2) -> e1, LinkedHashMap::new));
		final Map<Integer, List<TBinaryQuestion>> bqMap = binaryQuestions.stream()
				.collect(Collectors.groupingBy(k -> k.questionId, LinkedHashMap::new, Collectors.toList()));

		final TemplateXLSX tmpl = new TemplateXLSX();
		tmpl.setSettings(settings);
		variables.forEach(tmpl::addSkill);
		tmpl.addQuestions(questions, ansMap, bqMap);

		return tmpl.structure();
	}
}
