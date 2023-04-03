package ch.idsia.adaptive.backend.persistence.dao;

import ch.idsia.adaptive.backend.persistence.model.Session;
import ch.idsia.adaptive.backend.persistence.model.State;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    24.11.2020 17:43
 */
@Repository
public interface StatesRepository extends CrudRepository<State, Long> {

	State findFirstBySessionOrderByCreationDesc(Session s);

	List<State> findAllBySessionOrderByCreationAsc(Session s);

	List<State> findAllBySessionIn(List<Session> sessions);

	Integer deleteBySessionIn(List<Session> sessions);
}
