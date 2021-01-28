package ch.idsia.adaptive.backend.persistence.dao;

import ch.idsia.adaptive.backend.persistence.model.Admin;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    28.01.2021 09:59
 */
@Repository
public interface AdminRepository extends CrudRepository<Admin, Long> {

	Admin findByToken(String token);
}
