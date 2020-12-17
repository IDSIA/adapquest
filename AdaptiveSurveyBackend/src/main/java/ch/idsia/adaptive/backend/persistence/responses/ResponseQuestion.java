package ch.idsia.adaptive.backend.persistence.responses;

import ch.idsia.adaptive.backend.persistence.model.Question;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    16.12.2020 18:00
 */
@NoArgsConstructor
public class ResponseQuestion {

	public Long id;
	public String explanation = "";
	public String question = "";
	public List<ResponseAnswer> answers = new ArrayList<>();

	public Boolean isExample = false;
	public Boolean randomAnswers = false;

	public ResponseQuestion(Question question) {
		this.id = question.getId();
		this.explanation = question.getExplanation();
		this.question = question.getQuestion();

		this.isExample = question.getIsExample();
		this.randomAnswers = question.getRandomAnswers();

		this.answers = question.getAnswersAvailable().stream()
				.map(ResponseAnswer::new)
				.collect(Collectors.toList());
	}
}
