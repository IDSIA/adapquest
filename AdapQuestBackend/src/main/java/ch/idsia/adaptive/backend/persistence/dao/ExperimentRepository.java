package ch.idsia.adaptive.backend.persistence.dao;

import ch.idsia.adaptive.backend.persistence.model.Experiment;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    02.12.2021 15:08
 */
@Repository
public interface ExperimentRepository extends CrudRepository<Experiment, Long> {

	List<Experiment> findAllByOrderByCreationDesc();

	Experiment findByName(String name);
}
