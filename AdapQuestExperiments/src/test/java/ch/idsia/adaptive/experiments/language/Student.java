package ch.idsia.adaptive.experiments.language;

import ch.idsia.adaptive.backend.persistence.responses.ResponseState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

/**
 * Dummy class to identify all the answers of a student.
 */
public class Student extends HashMap<String, Integer> {
	public String token;
	public Integer i;
	public List<ResponseState> states = new ArrayList<>();

	public Student(String[] header, String[] answers) {
		IntStream.range(0, header.length)
				.forEach(i -> put(header[i], Integer.parseInt(answers[i].trim())));
	}

	public ResponseState last() {
		return states.get(states.size() - 1);
	}
}
