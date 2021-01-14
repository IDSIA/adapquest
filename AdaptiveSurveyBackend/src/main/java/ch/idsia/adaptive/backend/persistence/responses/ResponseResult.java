package ch.idsia.adaptive.backend.persistence.responses;

import ch.idsia.adaptive.backend.persistence.model.Session;
import ch.idsia.adaptive.backend.persistence.model.State;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    25.11.2020 11:50
 */
@NoArgsConstructor
public class ResponseResult {

	/**
	 * Total seconds.
	 */
	public Long seconds;

	/**
	 * Extra data.
	 */
	public LocalDateTime data;

	/**
	 * Has ended;
	 */
	public Boolean ended;

	/**
	 * Last state of the survey.
	 */
	public ResponseState state;

	public ResponseResult(Session session, State status) {
		data = session.getStartTime();
		seconds = session.getElapsedSeconds();
		ended = session.getEndTime() != null;
		state = new ResponseState(status);
	}
}
