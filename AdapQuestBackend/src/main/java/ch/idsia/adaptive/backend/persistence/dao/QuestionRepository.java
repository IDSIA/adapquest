package ch.idsia.adaptive.backend.persistence.dao;

import ch.idsia.adaptive.backend.persistence.model.Question;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    24.11.2020 17:43
 */
@Repository
public interface QuestionRepository extends CrudRepository<Question, Long> {

	Question findQuestionBySurveyIdAndId(Long survey, Long id);

	Question findQuestionBySurveyAccessCodeAndName(String key, String name);

}
