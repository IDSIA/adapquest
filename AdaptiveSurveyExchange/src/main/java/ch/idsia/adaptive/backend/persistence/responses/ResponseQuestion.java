package ch.idsia.adaptive.backend.persistence.responses;

import java.util.ArrayList;
import java.util.List;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    16.12.2020 18:00
 */
public class ResponseQuestion {

	public Long id;
	public String explanation = "";
	public String question = "";
	public List<ResponseAnswer> answers = new ArrayList<>();

	public Boolean isExample = false;
	public Boolean randomAnswers = false;

}
