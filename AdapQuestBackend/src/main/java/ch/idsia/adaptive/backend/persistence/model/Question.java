package ch.idsia.adaptive.backend.persistence.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Question implements Comparable<Question> {
	private static final Logger logger = LoggerFactory.getLogger(Question.class);

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
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
	@EqualsAndHashCode.Include
	private String question = "";

	/**
	 * Name of the variable controlled by this question.
	 */
	@Column
	@EqualsAndHashCode.Include
	private String name = "";

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
	 * If true, this will be considered a multiple-choice question. The relative {@link QuestionAnswer}s should have
	 * their own model variable associated.
	 */
	private Boolean multipleChoice = false;

	/**
	 * If true, and {@link #multipleChoice} is also true, only the evidence of checked (true) answers are considered by
	 * the inference engine.
	 */
	private Boolean yesOnly = false;

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
	@OneToMany(mappedBy = "question", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
	@OrderBy("id ASC")
	@JsonManagedReference
	private List<QuestionAnswer> answersAvailable;

	/**
	 * {@link Skill}s associated with this question.
	 */
	@ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
	@JoinColumn(name = "fk_question_skill")
	private Set<Skill> skills;

	/**
	 * {@link Survey} that include this question.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "fk_question_survey")
	@JsonBackReference
	private Survey survey;

	@Transient
	private Map<Integer, QuestionAnswer> qaMap = new HashMap<>();

	@Transient
	private Set<Integer> variables = new LinkedHashSet<>();

	@PostLoad
	public void mapVariables() {
		qaMap.putAll(answersAvailable.stream().collect(Collectors.toMap(
				k -> Objects.hash(k.getVariable(), k.getState()),
				v -> v,
				(a, b) -> a
		)));
		variables.addAll(answersAvailable.stream().map(QuestionAnswer::getVariable).collect(Collectors.toSet()));
	}

	public QuestionAnswer getQuestionAnswer(Integer variable, Integer state) {
		return qaMap.get(Objects.hash(variable, state));
	}

	public QuestionAnswer getQuestionAnswer(Integer index) {
		return answersAvailable.get(index);
	}

	public Question addAnswersAvailable(QuestionAnswer... answersAvailable) {
		this.answersAvailable = Arrays.stream(answersAvailable)
				.peek(a -> a.setQuestion(this))
				.collect(Collectors.toList());
		mapVariables();
		return this;
	}

	public Integer getVariable() {
		if (variables.size() > 1)
			logger.trace("requested single variable for multi-variable question: name={} qid={}", name, id);

		return new ArrayList<>(variables).get(0);
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
		return "Question{" + name + " " + skills + "}";
	}

}
