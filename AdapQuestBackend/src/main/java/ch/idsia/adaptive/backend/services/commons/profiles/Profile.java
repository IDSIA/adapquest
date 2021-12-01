package ch.idsia.adaptive.backend.services.commons.profiles;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    25.11.2021 10:46
 */
@Getter
public class Profile {

	final String name;
	final Map<String, Integer> skills = new LinkedHashMap<>();
	final Map<String, Integer> answers = new HashMap<>();

	@Setter
	Map<String, Map<String, double[]>> weights = new HashMap<>();

	@Setter
	Integer col;

	public Profile(String name, Integer col) {
		this.name = name;
		this.col = col;
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
		Profile profile = (Profile) o;
		return name.equals(profile.name);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name);
	}

}
