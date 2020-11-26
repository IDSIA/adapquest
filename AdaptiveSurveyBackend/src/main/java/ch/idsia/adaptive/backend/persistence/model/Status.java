package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.utils.MapStringDoubleArrayConverter;
import ch.idsia.adaptive.backend.persistence.utils.MapStringLongConverter;
import ch.idsia.adaptive.backend.persistence.utils.SetStringConverter;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 11:33
 */
@Entity
@Data
public class Status {

	/**
	 * If a skill is completed (no more questions) its name should be saved there there.
	 */
	@Transient
	@Convert(converter = SetStringConverter.class)
	public Set<String> skillCompleted;
	/**
	 * Mapping skill name to distribution.
	 */
	@Transient
	@Convert(converter = MapStringDoubleArrayConverter.class)
	public Map<String, double[]> state;
	@Id
	@GeneratedValue
	private Long id;
	/**
	 * Encoded Status saved.
	 */
	// TODO: save to file? Is this model dependent?
	private String status;
	/**
	 * Moment in time when this Status was created.
	 */
	private LocalDateTime creation;

	/**
	 * Total answers given for each skill, mapped by skill name.
	 */
	@Transient
	@Convert(converter = MapStringLongConverter.class)
	private Map<String, Long> questionsPerSkill;

	/**
	 * Total answers given.
	 */
	private Long questionsTotal;

	/**
	 * Session associated with this Status.
	 */
	@ManyToOne
	private Session session;

}
