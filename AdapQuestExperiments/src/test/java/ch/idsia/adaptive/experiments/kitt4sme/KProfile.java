package ch.idsia.adaptive.experiments.kitt4sme;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    15.10.2021 18:23
 */
public class KProfile {

	final String name;
	final Map<String, Integer> skills = new LinkedHashMap<>();
	final Map<String, Integer> answers = new HashMap<>();
	Map<String, Map<String, double[]>> weights;

	public KProfile(String name) {
		this.name = name;
	}

	private String key(String qid, String aid) {
		return qid + "$" + aid;
	}

	public void add(String skill, Integer value) {
		skills.put(skill, value);
	}

	public void add(String qid, String aid, Integer ans) {
		this.answers.put(key(qid, aid), ans);
	}

	public int answer(String qid, String aid) {
		final String key = key(qid, aid);
		if (answers.containsKey(key))
			return answers.get(key);
		return -1;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		KProfile kProfile = (KProfile) o;
		return name.equals(kProfile.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

	public static List<KProfile> read() throws Exception {
		// reading answers and profiles
		final Map<Integer, String> names = new HashMap<>();
		final Map<String, KProfile> profiles = new HashMap<>();

		try (FileInputStream fis = new FileInputStream("AdaptiveQuestionnaire.xlsx")) {

			final Workbook workbook = new XSSFWorkbook(fis);

			final Sheet sheetSkills = workbook.getSheet("Pilot Skill");
			final Sheet sheetPilotAnswers = workbook.getSheet("Pilot+Interviews Answers");
			final Sheet sheetBinaryQuestions = workbook.getSheet("Binary questions");

			// profiles parsing
			for (Row row : sheetSkills) {
				if (row.getRowNum() == 0) {
					for (int j = 2; j < row.getLastCellNum(); j++) {
						if (row.getCell(j) == null)
							continue;
						final String profile = row.getCell(j).getStringCellValue();
						names.put(j, profile);
						profiles.put(profile, new KProfile(profile));
					}

					continue;
				}

				if (row.getCell(1) == null)
					break;

				final String n = row.getCell(1).getStringCellValue();
				for (Integer j : names.keySet()) {
					final int s = Double.valueOf(row.getCell(j).getNumericCellValue()).intValue();
					profiles.get(names.get(j)).add(n, s);
				}
			}

			// answers parsing
			names.clear();
			int Q = -1;
			for (Row row : sheetPilotAnswers) {
				if (row.getRowNum() == 0)
					continue;

				if (row.getRowNum() == 1) {
					for (int j = 5, k = 0; k < profiles.size(); j++, k++) {
						final String profile = row.getCell(j).getStringCellValue();
						names.put(j, profile);
					}

					continue;
				}

				if (row.getCell(3) == null || row.getCell(3).toString().isEmpty()) {
					break;
				}

				final int A = Double.valueOf(row.getCell(3).getNumericCellValue()).intValue();
				if (A == 1) {
					Q = Double.valueOf(row.getCell(0).getNumericCellValue()).intValue();
				}

				final int a = A;
				final int q = Q;

				for (Integer j : names.keySet()) {
					profiles.get(names.get(j)).add("Q" + q, "A" + a, Double.valueOf(row.getCell(j).getNumericCellValue()).intValue());
				}
			}

//		for (String name : names.values()) {
//			System.out.println(name);
//			System.out.println("Skills: " + Arrays.toString(profiles.get(name)));
//			final int[][] ans = answers.get(name);
//			for (int[] an : ans) {
//				System.out.println(Arrays.toString(an));
//			}
//		}

			String qid = "";
			int iValQuestionId = 0, iValAnswerId = 0, iStartSkills = 0, limit = 0;

			final List<String> skills = new ArrayList<>();
			final Map<String, Map<String, double[]>> weights = new ConcurrentHashMap<>();

			for (Row row : sheetBinaryQuestions) {
				final int i = row.getRowNum();
				if (i == 0) {
					// parse header
					for (int j = row.getFirstCellNum(); j < row.getLastCellNum(); j++) {
						switch (row.getCell(j).getStringCellValue().toUpperCase()) {
							case "Q_ID":
								iValQuestionId = j;
								break;
							case "A_ID":
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

			profiles.values().forEach(p -> p.weights = weights);
		}
		return new ArrayList<>(profiles.values());
	}
}
