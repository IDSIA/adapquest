package ch.idsia.adaptive.backend.persistence.model;

import ch.idsia.adaptive.backend.persistence.utils.MapStringDoubleArrayConverter;
import ch.idsia.adaptive.backend.persistence.utils.MapStringLongConverter;
import ch.idsia.adaptive.backend.persistence.utils.SetStringConverter;
import lombok.Data;
import lombok.experimental.Accessors;

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
@Accessors(chain = true)
public class Status {

	@Id
	@GeneratedValue
	private Long id;

	/**
	 * Encoded Status saved.
	 */
	// TODO: save to file? Is this model dependent?
	private String status;

	/**
	 * If a skill is completed (no more questions) its name should be saved there there.
	 */
	@Transient
	@Convert(converter = SetStringConverter.class)
	public Set<String> skillCompleted;

	/**
	 * Total answers given for each skill, mapped by skill name.
	 */
	@Transient
	@Convert(converter = MapStringLongConverter.class)
	private Map<String, Long> questionsPerSkill;
	/**
	 * Mapping skill name to distribution.
	 */
	@Transient
	@Convert(converter = MapStringDoubleArrayConverter.class)
	public Map<String, double[]> state;
	/**
	 * Moment in time when this Status was created.
	 */
	private LocalDateTime creation = LocalDateTime.now();

	/**
	 * Total answers given.
	 */
	private Long questionsTotal;

	/**
	 * Session associated with this Status.
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	private Session session;

}
