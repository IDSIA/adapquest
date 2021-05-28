package ch.idsia.adaptive.experiments.alloy;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    26.02.2021 12:45
 */
class Answers extends HashMap<String, String> {
	public String id;

	interface Clean {
		String $(String text);
	}

	static Clean k = text -> {
		if (text.endsWith("_0"))
			return text.replace("_0", "");
		return text;
	};
	static Clean v = text -> {
		if (text.endsWith(".0"))
			return text.replace(".0", "");
		if (text.contains(","))
			return text.replace("(", "_").replace(",", "_");
		return text;
	};

	static List<Answers> get() throws Exception {
		final List<String> lines = Files.readAllLines(Paths.get("alloy.answers.tsv"));
		final String[] header = lines.get(0).split("\t");
		final List<Answers> answers = new ArrayList<>();

		for (int i = 1; i < lines.size(); i++) {
			final String[] tokens = lines.get(i).split("\t");
			Answers a = new Answers();
			a.id = tokens[0];
			for (int j = 1; j < header.length; j++) {
				if (j < tokens.length) {
					final String key = header[j];
					final String val = tokens[j];

					a.put(key, v.$(val));
					a.put(k.$(key), v.$(val));
				}
			}
			answers.add(a);
		}

		return answers;
	}
}
