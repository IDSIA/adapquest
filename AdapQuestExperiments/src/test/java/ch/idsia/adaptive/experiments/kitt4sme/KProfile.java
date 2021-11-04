package ch.idsia.adaptive.experiments.kitt4sme;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileInputStream;
import java.util.*;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    15.10.2021 18:23
 */
public class KProfile {

	final String name;
	final Map<String, Integer> skills = new LinkedHashMap<>();
	final Map<String, Integer> answers = new HashMap<>();

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
			final Sheet sheetPilotAnswers = workbook.getSheet("Pilot Answers");

			// profiles parsing
			for (Row row : sheetSkills) {
				if (row.getRowNum() == 0) {
					for (int j = 2; j < row.getLastCellNum(); j++) {
						final String profile = row.getCell(j).getStringCellValue();
						names.put(j, profile);
						profiles.put(profile, new KProfile(profile));
					}

					continue;
				}

				final String n = row.getCell(1).getStringCellValue();
				for (int j = 2, k = 0; k < names.size(); j++, k++) { // two columns on the left
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

				if (row.getRowNum() == 107) {
					break;
				}

				final int A = Double.valueOf(row.getCell(3).getNumericCellValue()).intValue();
				if (A == 1) {
					Q = Double.valueOf(row.getCell(0).getNumericCellValue()).intValue();
				}

				final int a = A;
				final int q = Q;

				for (int j = 5, k = 0; k < names.size(); j++, k++) {
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

			return new ArrayList<>(profiles.values());
		}
	}

}
