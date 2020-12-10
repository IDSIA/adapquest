package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.List;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 11:45
 */
@Entity
@Data
@Accessors(chain = true)
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Explanation text.
	 */
	private String explanation;

	/**
	 * Question text.
	 */
	private String question;

	/**
	 * Available answers for this multiple choice question.
	 */
	@OneToMany(cascade = CascadeType.ALL)
	@OrderBy("id asc")
	private List<QuestionAnswer> answersAvailable;

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
	 * Given answers to this question.
	 */
	@OneToMany
	private Set<Answer> answers;

	/**
	 * Skill associated with this question.
	 */
	@ManyToOne(cascade = CascadeType.ALL)
	private Skill skill;

	/**
	 * Difficulty associated with this question.
	 */
	@ManyToOne(cascade = CascadeType.ALL)
	private QuestionLevel level;

	/**
	 * Survey that include this question.
	 */
	@ManyToOne
	private Survey survey;
}
