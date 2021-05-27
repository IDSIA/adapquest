package ch.idsia.adaptive.backend.persistence.dao;

import ch.idsia.adaptive.backend.persistence.model.Answer;
import ch.idsia.adaptive.backend.persistence.model.Session;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    24.11.2020 17:43
 */
@Repository
public interface AnswerRepository extends CrudRepository<Answer, Long> {

	List<Answer> findAllBySessionTokenOrderByCreationAsc(String token);

	Integer deleteBySessionIn(List<Session> sessions);
}
