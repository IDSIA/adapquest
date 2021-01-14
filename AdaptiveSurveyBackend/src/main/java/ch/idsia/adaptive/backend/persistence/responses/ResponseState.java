package ch.idsia.adaptive.backend.persistence.responses;

import ch.idsia.adaptive.backend.persistence.model.State;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    11.12.2020 09:29
 */
@NoArgsConstructor
public class ResponseState {

	/**
	 * Set of available skills.
	 */
	public Set<String> skills = new HashSet<>();

	/**
	 * Distribution for each skill.
	 */
	public Map<String, double[]> skillDistribution = new HashMap<>();

	/**
	 * Distribution for each skill.
	 */
	public Map<String, Double> entropyDistribution = new HashMap<>();

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

	public ResponseState(State state) {
		skillDistribution = state.getState();
		entropyDistribution = state.getEntropy();
		skillCompleted = state.getSkillCompleted();
		questionsPerSkill = state.getQuestionsPerSkill();
		creationTime = state.getCreation();
		totalAnswers = state.getTotalAnswers();
		skills.addAll(skillDistribution.keySet());
	}
}
