package ch.idsia.adaptive.backend.persistence.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import java.time.LocalDateTime;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    02.12.2021 15:03
 */
@Entity
@Data
@Accessors(chain = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Experiment {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@EqualsAndHashCode.Include
	private Long id;

	private String name;
	private String result;

	// failed, running, completed
	private String status;

	private LocalDateTime creation = LocalDateTime.now();
	private LocalDateTime completion = null;

	private Boolean completed = false;

}
