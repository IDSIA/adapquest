package ch.idsia.adaptive.backend.persistence.responses;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    17.12.2020 16:26
 */
public class ResponseAnswer {

	/**
	 * Id of the answer (include this in the {@link ch.idsia.adaptive.backend.persistence.requests.RequestAnswer}).
	 */
	public Long id;

	/**
	 * Text to show to the humans.
	 */
	public String text;

	/**
	 * Index associated with the model.
	 */
	public Integer state;

}
