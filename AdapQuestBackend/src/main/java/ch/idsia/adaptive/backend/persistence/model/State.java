package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.utils.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    25.11.2020 11:33
 */
@Entity
@Data
@Accessors(chain = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class State {

	/**
	 * Mapping of skills.
	 */
	@Convert(converter = MapStringSkillConverter.class)
	@Column(name = "skills", length = 1023)
	public Map<String, Skill> skills = new HashMap<>();

	/**
	 * Mapping skill name to entropy.
	 */
	@Convert(converter = MapStringDoubleConverter.class)
	@Column(name = "entropy", length = 1023)
	public Map<String, Double> entropy = new HashMap<>();

	/**
	 * If a skill is completed (no more questions) its name should be saved there there.
	 */
	@Convert(converter = SetStringConverter.class)
	@Column(name = "skillCompleted", length = 1023)
	public Set<String> skillCompleted = new HashSet<>();

	/**
	 * Encoded Status saved.
	 */
	// TODO: save to file? Is this model dependent?
	private String status;

	/**
	 * Mapping skill name to distribution.
	 */
	@Convert(converter = MapStringDoubleArrayConverter.class)
	@Column(name = "state", length = 1023)
	public Map<String, double[]> state = new HashMap<>();
	@Id
	@GeneratedValue
	@EqualsAndHashCode.Include
	private Long id;

	/**
	 * Moment in time when this Status was created.
	 */
	private LocalDateTime creation = LocalDateTime.now();

	/**
	 * Total answers given.
	 */
	private Integer totalAnswers;

	/**
	 * Total answers given for each skill, mapped by skill name.
	 */
	@Convert(converter = MapStringLongConverter.class)
	@Column(name = "questionsPerSkill", length = 1023)
	private Map<String, Long> questionsPerSkill = new HashMap<>();

	/**
	 * Session associated with this Status.
	 */
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "fk_state_session")
	private Session session;

}
