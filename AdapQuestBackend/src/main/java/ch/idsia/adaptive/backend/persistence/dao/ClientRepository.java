package ch.idsia.adaptive.backend.persistence.dao;

import ch.idsia.adaptive.backend.persistence.model.Client;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    28.01.2021 09:59
 */
@Repository
public interface ClientRepository extends CrudRepository<Client, Long> {

	Client findClientByKey(String key);

	Client findClientByUsernameOrEmail(String username, String email);
}
