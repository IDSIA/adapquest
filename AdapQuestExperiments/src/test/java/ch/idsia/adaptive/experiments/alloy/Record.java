package ch.idsia.adaptive.experiments.alloy;

import ch.idsia.adaptive.backend.persistence.responses.ResponseSkill;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    26.02.2021 12:45
 */
class Record {
	Double qscore;
	ResponseState state;
	String id;
	Long qid;
	Long aid;
	String qtext;
	String atext;
	Long startTime;
	Long endTime;
	Long elapsedTime;

	static List<String> toCSV(List<Record> records) {
		final List<String> lines = new ArrayList<>();

		final List<String> header = new ArrayList<>();
		header.add("id");
		header.add("qid");
		header.add("qtext");
		header.add("qscore");
		header.add("aid");
		header.add("atext");
		header.add("answers");
		header.add("creationTime");
		header.add("startTime");
		header.add("endTime");
		header.add("elapsedTime");

		for (ResponseSkill skill : records.get(0).state.skills) {
			header.add("P(" + skill.name + "=0)");
			header.add("P(" + skill.name + "=1)");
			header.add("H(" + skill.name + ")");
		}

		lines.add(String.join("\t", header));

		for (Record record : records) {
			final ResponseState state = record.state;

			final List<String> line = new ArrayList<>();
			line.add(record.id);
			line.add("" + record.qid);
			line.add("" + record.qtext);
			line.add("" + record.qscore);
			line.add("" + record.aid);
			line.add("" + record.atext);
			line.add("" + state.totalAnswers);
			line.add("" + state.creationTime.toString());
			line.add("" + record.startTime);
			line.add("" + record.endTime);
			line.add("" + record.elapsedTime);

			for (ResponseSkill skill : state.skills) {
				line.add("" + state.skillDistribution.get(skill.name)[0]);
				line.add("" + state.skillDistribution.get(skill.name)[1]);
				line.add("" + state.scoreDistribution.get(skill.name));
			}

			lines.add(String.join("\t", line));
		}

		return lines;
	}
}
