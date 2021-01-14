package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 11:34
 */
@Entity
@Data
@Accessors(chain = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Answer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	/**
	 * Moment in time this answer was created.
	 */
	private LocalDateTime creation = LocalDateTime.now();

	/**
	 * Correctness of this answer.
	 */
	private Boolean isCorrect = false;

	/**
	 * The answer given.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "fk_questionAnswer")
	private QuestionAnswer questionAnswer;

	/**
	 * Question of which this answer is for.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "fk_question")
	private Question question;

	/**
	 * Session associated with this answer.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_session")
	private Session session;

	public Long getQuestionId() {
		return question.getId();
	}

	public Skill getSkill() {
		return question.getSkill();
	}

	@Override
	public String toString() {
		return String.format("Answer to Question %d %s %s: %s", getQuestionId(), getSkill(), getQuestion().getLevel(), isCorrect);
	}
}
