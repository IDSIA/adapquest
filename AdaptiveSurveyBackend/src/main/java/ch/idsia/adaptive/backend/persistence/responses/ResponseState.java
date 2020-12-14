package ch.idsia.adaptive.backend.persistence.responses;

import ch.idsia.adaptive.backend.persistence.model.Status;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    11.12.2020 09:29
 */
public class ResponseState {

	public final Map<String, double[]> skillDistribution;
	public final Set<String> skillCompleted;
	public final Map<String, Long> questionsPerSkill;
	public final LocalDateTime creationTime;
	public final Long questionsTotal;

	public ResponseState(Status status) {
		skillDistribution = status.getState();
		skillCompleted = status.getSkillCompleted();
		questionsPerSkill = status.getQuestionsPerSkill();
		creationTime = status.getCreation();
		questionsTotal = status.getQuestionsTotal();
	}
}
