package ch.idsia.adaptive.backend.persistence.dao;

import ch.idsia.adaptive.backend.persistence.model.Survey;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    24.11.2020 17:43
 */
@Repository
public interface SurveyRepository extends CrudRepository<Survey, Long> {

	Survey findByAccessCode(String accessCode);

	@Query("SELECT accessCode FROM Survey")
	Collection<String> findAllAccessCodes();

	@Query("SELECT accessCode FROM Survey WHERE isAdaptive = true")
	Collection<String> findAllAccessCodesAdaptive();
}
