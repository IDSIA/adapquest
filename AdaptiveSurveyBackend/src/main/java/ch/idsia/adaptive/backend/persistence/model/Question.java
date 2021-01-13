package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 11:45
 */
@Entity
@Data
@Accessors(chain = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	/**
	 * Explanation text.
	 */
	private String explanation = "";

	/**
	 * Question text.
	 */
	private String question = "";

	/**
	 * Weight of this question in points.
	 */
	private Double weight = 1.0;

	/**
	 * If true this question will be shown as an example question that cannot be answered.
	 */
	private Boolean isExample = false;

	/**
	 * If true the order of the answers will be random.
	 */
	private Boolean randomAnswers = false;

	/**
	 * Available answers for this multiple choice question.
	 */
	@OneToMany(mappedBy = "question", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@OrderBy("id ASC")
	private List<QuestionAnswer> answersAvailable;

	/**
	 * Given answers to this question.
	 */
	@OneToMany(mappedBy = "question", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private Set<Answer> answers;

	/**
	 * Skill associated with this question.
	 */
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "fk_skill")
	private Skill skill;

	/**
	 * Difficulty associated with this question.
	 */
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JoinColumn(name = "fk_level")
	private QuestionLevel level;

	/**
	 * Survey that include this question.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_survey")
	private Survey survey;

	public Question addAnswersAvailable(QuestionAnswer... answersAvailable) {
		this.answersAvailable = Arrays.stream(answersAvailable)
				.peek(a -> a.setQuestion(this))
				.collect(Collectors.toList());
		return this;
	}

	@Override
	public String toString() {
		return "Question{" +
				"id=" + id +
				", question=" + question +
				", skill=" + skill +
				'}';
	}
}
