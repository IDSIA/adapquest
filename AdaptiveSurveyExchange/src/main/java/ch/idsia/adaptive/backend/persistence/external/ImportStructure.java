package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    19.01.2021 12:02
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class ImportStructure {

	public SurveyStructure survey = null;
	public List<SkillStructure> skills = new ArrayList<>();
	public List<QuestionStructure> questions = new ArrayList<>();
	public Optional<ModelStructure> model = Optional.empty();
	public Optional<String> modelData = Optional.empty(); // has priority

}
