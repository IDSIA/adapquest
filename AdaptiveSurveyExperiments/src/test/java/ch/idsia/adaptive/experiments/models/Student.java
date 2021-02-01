package ch.idsia.adaptive.experiments.models;

import ch.idsia.adaptive.backend.persistence.responses.ResponseState;

import java.util.HashMap;
import java.util.stream.IntStream;

/**
 * Dummy class to identify all the answers of a student.
 */
public class Student extends HashMap<String, Integer> {
	public ResponseState state;

	public Student(String[] header, String[] answers) {
		IntStream.range(0, header.length)
				.forEach(i -> put(header[i], Integer.parseInt(answers[i].trim())));
	}
}
