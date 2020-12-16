package ch.idsia.adaptive.backend.persistence.responses;

import ch.idsia.adaptive.backend.persistence.model.Question;
import ch.idsia.adaptive.backend.persistence.model.QuestionAnswer;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    16.12.2020 18:00
 */
@NoArgsConstructor
public class ResponseQuestion {

	public String explanation = "";
	public String question = "";
	public Map<Long, String> answers = new HashMap<>();

	public Boolean isExample = false;
	public Boolean randomAnswers = false;

	public ResponseQuestion(Question question) {
		this.explanation = question.getExplanation();
		this.question = question.getQuestion();

		this.isExample = question.getIsExample();
		this.randomAnswers = question.getRandomAnswers();

		this.answers = question.getAnswersAvailable().stream()
				.collect(Collectors.toMap(
						QuestionAnswer::getId,
						QuestionAnswer::getText
				));
	}
}
