package ch.idsia.adaptive.backend.persistence.dao;

import ch.idsia.adaptive.backend.persistence.model.QuestionAnswer;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    24.11.2020 17:43
 */
@Repository
public interface QuestionAnswerRepository extends CrudRepository<QuestionAnswer, Long> {

	QuestionAnswer findByIdAndQuestionId(Long answerId, Long questionId);

}
