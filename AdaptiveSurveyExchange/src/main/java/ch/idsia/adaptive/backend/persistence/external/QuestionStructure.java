package ch.idsia.adaptive.backend.persistence.external;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    19.01.2021 12:03
 */
@Data
@NoArgsConstructor
@Accessors(chain = true)
public class QuestionStructure {

	public String skill = "";
	public String question = "";
	public String explanation = "";
	public String name = "";
	public Integer variable = -1;
	public Double weight = 1.;
	public Boolean example = false;
	public Boolean randomAnswers = false;
	public List<AnswerStructure> answers = new ArrayList<>();

}
