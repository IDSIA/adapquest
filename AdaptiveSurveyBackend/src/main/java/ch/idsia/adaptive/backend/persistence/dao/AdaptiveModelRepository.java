package ch.idsia.adaptive.backend.persistence.dao;

import ch.idsia.adaptive.backend.persistence.model.AdaptiveModel;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    24.11.2020 17:43
 */
@Repository
public interface AdaptiveModelRepository extends CrudRepository<AdaptiveModel, Long> {

	AdaptiveModel findAllById(Long id);

}
