package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.utils.ListIntegerConverter;
import com.fasterxml.jackson.annotation.JsonBackReference;
import gnu.trove.map.TIntIntMap;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    26.11.2020 11:59
 */
@Entity
@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class QuestionAnswer {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	/**
	 * Text for this answer.
	 */
	@Column(length = 4096)
	private String text;

	/**
	 * Name of the variable controlled by this question.
	 */
	@EqualsAndHashCode.Include
	private String name;

	/**
	 * True if this is a correct answer.
	 */
	private Boolean correct = false;

	/**
	 * If true, this answer cannot be selected.
	 */
	private Boolean hidden = false;

	/**
	 * Refers to the state of the the model associated with this answer.
	 */
	private Integer state = -1;

	/**
	 * If the {@link Question} is a multiple-choice, this will be the reference of the variable in the model. Note that
	 * for multiple-choice answers, state 1 is checked true while state 0 is checked false.
	 */
	private Integer variable = -1;

	/**
	 * If true, this will generate an evidence directly on the nodes specified by {@link #directEvidenceVariables} in the
	 * state specified by {@link #directEvidenceStates}.
	 */
	private Boolean directEvidence = false;

	/**
	 * If {@link #directEvidence} is true, then this will also set an evidence on the nodes identified by these
	 * variables with the states specified in {@link #directEvidenceStates}.
	 */
	@Convert(converter = ListIntegerConverter.class)
	private List<Integer> directEvidenceVariables = new ArrayList<>();

	/**
	 * If {@link #directEvidence} is true, then this will also set an evidence on the nodes identified by
	 * {@link #directEvidenceVariables} with these states.
	 */
	@Convert(converter = ListIntegerConverter.class)
	private List<Integer> directEvidenceStates = new ArrayList<>();

	@OneToMany(mappedBy = "questionAnswer", fetch = FetchType.EAGER)
	private List<Answer> answers;

	@ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
	@JoinColumn(name = "fk_questionAnswer_question")
	@JsonBackReference
	private Question question;

	public QuestionAnswer(String text) {
		this.text = text;
	}

	public QuestionAnswer(String text, Boolean correct) {
		this.text = text;
		this.correct = correct;
	}

	public QuestionAnswer(String text, Integer variable, Integer state) {
		this.text = text;
		this.state = state;
		this.variable = variable;
	}

	public void observe(TIntIntMap observations) {
		if (variable >= 0)
			observations.put(variable, state);

		if (directEvidence) {
			final List<Integer> vars = getDirectEvidenceVariables();
			final List<Integer> states = getDirectEvidenceStates();

			for (int i = 0; i < vars.size(); i++) {
				observations.put(vars.get(i), states.get(i));
			}
		}
	}

}
