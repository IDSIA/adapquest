package ch.idsia.adaptive.backend.persistence.responses;

import ch.idsia.adaptive.backend.persistence.model.Status;
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

	public Map<String, double[]> skillDistribution = new HashMap<>();
	public Set<String> skillCompleted = new HashSet<>();
	public Map<String, Long> questionsPerSkill = new HashMap<>();
	public LocalDateTime creationTime = LocalDateTime.now();
	public Integer totalAnswers = 0;

	public ResponseState(Status status) {
		skillDistribution = status.getState();
		skillCompleted = status.getSkillCompleted();
		questionsPerSkill = status.getQuestionsPerSkill();
		creationTime = status.getCreation();
		totalAnswers = status.getTotalAnswers();
	}
}
