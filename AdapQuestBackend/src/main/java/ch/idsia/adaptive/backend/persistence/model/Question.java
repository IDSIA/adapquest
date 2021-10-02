package ch.idsia.adaptive.backend.persistence.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    25.11.2020 11:45
 */
@Entity
@Data
@Accessors(chain = true)
public class Question implements Comparable<Question> {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/**
	 * Explanation text.
	 */
	@Column(length = 4096)
	private String explanation = "";

	/**
	 * Question text.
	 */
	@Column(length = 4096)
	private String question = "";

	/**
	 * Name of the variable controlled by this question.
	 */
	private String name = "";

	/**
	 * Refers to the state of the the model associated with this answer.
	 */
	private Integer variable;

	/**
	 * Weight of this question in points.  Used for sorting from lower to high.
	 */
	private Double weight = 1.;

	/**
	 * If true this question will be shown as an example question that cannot be answered.
	 */
	private Boolean isExample = false;

	/**
	 * If true the order of the answers will be random.
	 */
	private Boolean randomAnswers = false;

	/**
	 * If true, this question will be forced to be asked before the adaptive engine starts.
	 */
	private Boolean mandatory = false;

	/**
	 * If true, this will be considered a multiple-choice question. The relative {@link QuestionAnswer} should have
	 * their own model variable associated.
	 */
	private Boolean multipleChoice = false;

	/**
	 * If true, this question influence a number of {@link Skill} greater than 1.
	 */
	private Boolean multipleSkills = false;

	/**
	 * Score assigned to this question.
	 */
	@Transient
	private Double score = Double.NaN;

	/**
	 * Available answers for this multiple choice question.
	 */
	@OneToMany(mappedBy = "question", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@OrderBy("id ASC")
	@JsonManagedReference
	private List<QuestionAnswer> answersAvailable;

	/**
	 * Given answers to this question.
	 */
	@OneToMany(mappedBy = "question", fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	private Set<Answer> answers;

	/**
	 * Skills associated with this question.
	 */
	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
	@JoinColumn(name = "fk_question_skill")
	private Set<Skill> skills;

	/**
	 * Survey that include this question.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_question_survey")
	@JsonBackReference
	private Survey survey;

	public Question addAnswersAvailable(QuestionAnswer... answersAvailable) {
		this.answersAvailable = Arrays.stream(answersAvailable)
				.peek(a -> a.setQuestion(this))
				.collect(Collectors.toList());
		return this;
	}

	public Question addSkills(Collection<Skill> skills) {
		if (Objects.isNull(this.skills))
			this.skills = new HashSet<>();

		this.skills.addAll(skills);
		return this;
	}

	public Question setSkill(Skill skill) {
		if (Objects.isNull(this.skills))
			this.skills = new HashSet<>();

		this.skills.add(skill);
		return this;
	}

	@Override
	public int compareTo(Question other) {
		return Double.compare(this.weight, other.weight);
	}

	@Override
	public String toString() {
		return "Question{" +
				"id=" + id +
				", skill=" + skills +
				", name=" + name +
				", weight=" + weight +
				'}';
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Question question1 = (Question) o;
		return question.equals(question1.question) && name.equals(question1.name) && variable.equals(question1.variable);
	}

	@Override
	public int hashCode() {
		return Objects.hash(question, name, variable);
	}

}
