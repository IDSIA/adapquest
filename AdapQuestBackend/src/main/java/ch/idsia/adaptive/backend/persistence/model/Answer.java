package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    25.11.2020 11:34
 */
@Entity
@Data
@NoArgsConstructor
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
	@JoinColumn(name = "fk_answer_questionAnswer")
	private QuestionAnswer questionAnswer;

	/**
	 * Session associated with this answer.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_answer_session")
	private Session session;

	public Answer(QuestionAnswer questionAnswer) {
		this.questionAnswer = questionAnswer;
	}

	public Question getQuestion() {
		return questionAnswer.getQuestion();
	}

	public Long getQuestionId() {
		return getQuestion().getId();
	}

	public Set<Skill> getSkills() {
		return getQuestion().getSkills();
	}

	@Override
	public String toString() {
		return String.format("Answer to Question %d %s %s: %s", getQuestionId(), getSkills(), getQuestion().getName(), isCorrect);
	}
}
