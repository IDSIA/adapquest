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

import static ch.idsia.adaptive.backend.services.templates.Settings.*;
import static java.lang.Math.max;
import static java.lang.Math.min;


/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    03.11.2021 08:45
 */
// TODO: this is a template for multi-choice only, we also need a mixed single-choice version!
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
	private double INHIBITOR_DEFAULT_VALUE = 0.6;

	private boolean OVERRIDE_INHIBITOR_VALUE_WITH_DEFAULT = true;

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
		final String K_INHIBITOR_DEFAULT_VALUE = "INHIBITOR_DEFAULT_VALUE";
		final String K_OVERRIDE_INHIBITOR_VALUE_WITH_DEFAULT = "INHIBITOR_OVERRIDE_VALUE_WITH_DEFAULT";

		if (settings.containsKey(K_EPS))
			eps = settings.get(K_EPS).getNumericCellValue();
		if (settings.containsKey(K_INHIBITOR_MAX_VALUE))
			INHIBITOR_MAX_VALUE = settings.get(K_INHIBITOR_MAX_VALUE).getNumericCellValue();
		if (settings.containsKey(K_INHIBITOR_MIN_VALUE))
			INHIBITOR_MIN_VALUE = settings.get(K_INHIBITOR_MIN_VALUE).getNumericCellValue();
		if (settings.containsKey(K_INHIBITOR_DEFAULT_VALUE))
			INHIBITOR_DEFAULT_VALUE = settings.get(K_INHIBITOR_DEFAULT_VALUE).getNumericCellValue();
		if (settings.containsKey(K_OVERRIDE_INHIBITOR_VALUE_WITH_DEFAULT))
			OVERRIDE_INHIBITOR_VALUE_WITH_DEFAULT = settings.get(K_OVERRIDE_INHIBITOR_VALUE_WITH_DEFAULT).getBooleanCellValue();

		if (settings.containsKey(ACCESS_CODE))
			survey.setAccessCode(settings.get(ACCESS_CODE).toString());

		if (settings.containsKey(ACCESS_CODE))
			survey.setAccessCode(settings.get(ACCESS_CODE).toString());
		if (settings.containsKey(LANGUAGE))
			survey.setLanguage(settings.get(LANGUAGE).toString());
		if (settings.containsKey(SURVEY_DESCRIPTION))
			survey.setDescription(settings.get(SURVEY_DESCRIPTION).toString());
		if (settings.containsKey(DURATION))
			survey.setDuration(Double.valueOf(settings.get(DURATION).getNumericCellValue()).longValue());
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
			survey.setQuestionPerSkillMin(Double.valueOf(settings.get(QUESTION_PER_SKILL_MIN).getNumericCellValue()).intValue());
		if (settings.containsKey(QUESTION_PER_SKILL_MAX))
			survey.setQuestionPerSkillMax(Double.valueOf(settings.get(QUESTION_PER_SKILL_MAX).getNumericCellValue()).intValue());
		if (settings.containsKey(QUESTION_TOTAL_MIN))
			survey.setQuestionTotalMin(Double.valueOf(settings.get(QUESTION_TOTAL_MIN).getNumericCellValue()).intValue());
		if (settings.containsKey(QUESTION_TOTAL_MAX))
			survey.setQuestionTotalMax(Double.valueOf(settings.get(QUESTION_TOTAL_MAX).getNumericCellValue()).intValue());
		if (settings.containsKey(SCORE_LOWER_THRESHOLD))
			survey.setScoreLowerThreshold(settings.get(SCORE_LOWER_THRESHOLD).getNumericCellValue());
		if (settings.containsKey(SCORE_UPPER_THRESHOLD))
			survey.setScoreUpperThreshold(settings.get(SCORE_UPPER_THRESHOLD).getNumericCellValue());
		if (settings.containsKey(GLOBAL_MEAN_SCORE_UPPER_THRESHOLD))
			survey.setGlobalMeanScoreUpperThreshold(settings.get(GLOBAL_MEAN_SCORE_UPPER_THRESHOLD).getNumericCellValue());
		if (settings.containsKey(GLOBAL_MEAN_SCORE_LOWER_THRESHOLD))
			survey.setGlobalMeanScoreLowerThreshold(settings.get(GLOBAL_MEAN_SCORE_LOWER_THRESHOLD).getNumericCellValue());
	}

	private QuestionStructure addQuestion(int qid, String qText, boolean mandatory, boolean yesOnly) {
		logger.debug("Adding question={}", qid);
		final QuestionStructure question = new QuestionStructure()
				.setName("Q" + qid)
				.setQuestion(qText)
				.setMandatory(mandatory)
				.setYesOnly(yesOnly)
				.setMultipleChoice(true)
				.setMultipleSkills(true)
				.setSkills(new HashSet<>())
				.setAnswers(new ArrayList<>());
		questions.add(question);

		return question;
	}

	private void addAnswer(QuestionStructure question, int aid, String text, Map<String, Double> values) {
		logger.debug("Adding answers={} to question={}", aid, question.getName());
		final List<Integer> parents = new ArrayList<>();
		final List<Double> inhibitors = new ArrayList<>();
		final List<String> ko = new ArrayList<>();

		values.forEach((k, v) -> {
			if (v > 0) {
				final double inh;
				if (OVERRIDE_INHIBITOR_VALUE_WITH_DEFAULT) {
					inh = INHIBITOR_DEFAULT_VALUE;
				} else {
					inh = min(INHIBITOR_MAX_VALUE, max(INHIBITOR_MIN_VALUE, 1.0 - v + eps));
				}
				parents.add(nameVariables.get(k));
				inhibitors.add(inh);
				question.getSkills().add(k);
			} else if (v < 0) {
				ko.add(k);
				question.getSkills().add(k);
			}
		});

		// noisy or
		final int nor = model.addVariable(2);

		final int[] p = parents.stream().mapToInt(x -> x).toArray();
		final double[] i = inhibitors.stream().mapToDouble(x -> x).toArray();

		parents.add(nor);
		model.addParents(nor, p);

		// displayed in multiple choice
		final AnswerStructure neg = new AnswerStructure("no", nor, 0).setName("A" + aid);
		final AnswerStructure pos = new AnswerStructure(text, nor, 1).setName("A" + aid);

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

		question.answers.add(neg);
		question.answers.add(pos);

		final int[] vars = parents.stream().mapToInt(x -> x).toArray();

		final BayesianNoisyOrFactor f_nor = BayesianFactorFactory.factory().domain(model.getDomain(vars)).noisyOr(p, i);
		factors.add(f_nor);
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
		final TemplateXLSX tmpl = new TemplateXLSX();

		try (final Workbook workbook = new XSSFWorkbook(new FileInputStream(path.toFile()))) {
			final Sheet sheetSettings = workbook.getSheet("Settings");
			final Sheet sheetBinaryQuestions = workbook.getSheet("Questions");

			// reading settings
			final Map<String, Cell> settings = new HashMap<>();

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

			tmpl.setSettings(settings);

			// reading model elicitation
			int qid, iValQuestionId = 0, iValQuestionText = 0, iValYesOnly = 0, iValMandatory = 0, iValAnswerId = 0, iValAnswerText = 0, iStartSkills = 0, limit = 0;

			QuestionStructure question = null;

			for (Row row : sheetBinaryQuestions) {
				final int i = row.getRowNum();
				if (i == 0) {
					// parse header
					for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
						switch (row.getCell(j).getStringCellValue().toUpperCase()) {
							case "QUESTION_ID":
								iValQuestionId = j;
								break;
							case "MANDATORY":
								iValMandatory = j;
								break;
							case "QUESTIONS_TEXT":
								iValQuestionText = j;
								break;
							case "YES_ONLY":
								iValYesOnly = j;
								break;
							case "ANSWER_ID":
								iValAnswerId = j;
								break;
							case "ANSWER_TEXT":
								iValAnswerText = j;
								break;
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
							tmpl.addSkill(row.getCell(j).getStringCellValue());
						}
					}
					limit = tmpl.skills.size();
					continue;
				}

				if (row.getCell(iValAnswerId) == null || row.getCell(iValAnswerId).toString().isEmpty())
					// skip empty rows or not model-related questions
					continue;

				if (row.getCell(iValQuestionId) != null && !row.getCell(iValQuestionId).toString().isEmpty()) {
					// update questions
					qid = Double.valueOf(row.getCell(iValQuestionId).getNumericCellValue()).intValue();
					final int man = Double.valueOf(row.getCell(iValMandatory).getNumericCellValue()).intValue();
					final String qText = row.getCell(iValQuestionText).getStringCellValue();
					final int yesOnly = Double.valueOf(row.getCell(iValYesOnly).getNumericCellValue()).intValue();

					logger.debug("Found new question={}", qid);

					question = tmpl.addQuestion(qid, qText, man == 1, yesOnly == 1);
				}

				final int aid = Double.valueOf(row.getCell(iValAnswerId).getNumericCellValue()).intValue();
				final String answerText = row.getCell(iValAnswerText).toString();
				final Map<String, Double> values = new HashMap<>();

				for (int j = iStartSkills, k = 0; k < limit; j++, k++) {
					final Cell c = row.getCell(j);
					final double v = c.toString().equals("KO") ? -1.0 : c.getNumericCellValue();
					values.put(tmpl.skills.get(k).getName(), v);
				}

				if (question != null)
					tmpl.addAnswer(question, aid, answerText, values);
			}
		}

		return tmpl.structure();
	}
}
