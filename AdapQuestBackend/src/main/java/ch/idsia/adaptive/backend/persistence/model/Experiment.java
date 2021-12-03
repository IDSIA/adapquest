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

	/**
	 * Filename on disk of the template used by this experiment. The file will be put in the data/experiments folder.
	 */
	private String name;
	/**
	 * Filename on disk of the results file created by this experiment. The file will be put in the data/results folder.
	 */
	private String result;

	/**
	 * Status of the job. Can be INIT, RUNNING, COMPLETED, or FAILED.
	 */
	private String status;

	/**
	 * When the initial template has been submitted.
	 */
	private LocalDateTime creation = LocalDateTime.now();
	/**
	 * When the result file has been created after a successful experiment.
	 */
	private LocalDateTime completion = null;

	/**
	 * True if the experiment is completed without errors.
	 */
	private Boolean completed = false;

}
