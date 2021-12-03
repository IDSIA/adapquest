package ch.idsia.adaptive.backend.persistence.responses;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    11.12.2020 09:29
 */
public class ResponseState {

	/**
	 * List of available skills.
	 */
	public List<ResponseSkill> skills = new ArrayList<>();

	/**
	 * Distribution for each skill.
	 */
	public Map<String, double[]> skillDistribution = new HashMap<>();

	/**
	 * Distribution for each skill.
	 */
	public Map<String, Double> scoreDistribution = new HashMap<>();

	/**
	 * Average of all the scores.
	 */
	public Double scoreAverage;

	/**
	 * Set of completed skills.
	 */
	public Set<String> skillCompleted = new HashSet<>();

	/**
	 * Number of questions done for each skill.
	 */
	public Map<String, Long> questionsPerSkill = new HashMap<>();

	/**
	 * When this state has been created.
	 */
	public LocalDateTime creationTime = LocalDateTime.now();

	/**
	 * Total number of answers given.
	 */
	public Integer totalAnswers = 0;


}
