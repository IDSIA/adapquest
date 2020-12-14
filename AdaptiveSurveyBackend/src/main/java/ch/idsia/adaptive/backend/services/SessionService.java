package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.dao.SessionRepository;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import ch.idsia.adaptive.backend.persistence.model.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    26.11.2020 15:01
 */
@Service
public class SessionService {
	private static final Logger logger = LogManager.getLogger(SessionService.class);

	// TODO: session can be not returned: this service will operate on the database without exposing the current session

	final SessionRepository repository;
	final SurveyRepository surveys;

	public SessionService(SessionRepository repository, SurveyRepository surveys) {
		this.repository = repository;
		this.surveys = surveys;
	}

	/**
	 * Check if exists a session with the given parameters. If it exists, returns this session, otherwise initializes
	 * and returns a new one. In this second case, the input {@link SurveyData} will be update with information from
	 * the session itself (token for the session, survey id, start time).
	 *
	 * @param data User's {@link SurveyData}, this will be updated
	 * @return
	 * @throws SessionException is the session already exists or the accessCode is not valid
	 */
	public Session registerNewSession(SurveyData data) throws SessionException {
		Session session = repository.findByToken(data.getToken());

		if (session == null) {
			logger.info("Registering new session with accessCode={} userAgent={} remoteAddress={}",
					data.getAccessCode(), data.getUserAgent(), data.getRemoteAddress()
			);

			Survey survey = surveys.findByAccessCode(data.getAccessCode());
			if (survey == null)
				throw new SessionException("Survey with accessCode=" + data.getAccessCode() + " does not exists!");

			session = new Session()
					.setUserAgent(data.getUserAgent())
					.setRemoteAddr(data.getRemoteAddress())
					.setSurvey(survey)
					.setToken(SurveyToken.create(data));

			repository.save(session);
			data.setFromSession(session);

			return session;
		} else {
			throw new SessionException("Session already exists for token " + data.getToken() + "!");
		}
	}

	/**
	 * Given the authentication data of a user, returns the session if exists.
	 *
	 * @param token session token
	 * @return The associated session if exists, otherwise null.
	 * @throws SessionException is the session does not exists
	 */
	public Session getSession(String token) throws SessionException {
		Session session = repository.findByToken(token);

		if (session == null)
			throw new SessionException("Session does not exists for token " + token + "!");

		session.setRestored(true);

		return session;
	}

	/**
	 * Set the end time for when the session ended.
	 *
	 * @param token session token
	 * @throws SessionException is the session does not exists
	 */
	public void endSurvey(String token) throws SessionException {
		Session session = getSession(token);
		session.setEndTime(LocalDateTime.now());
		repository.save(session);
	}

	/**
	 * Set the last given answer to the session identified by token.
	 *
	 * @param token  session token
	 * @param answer answer to save
	 * @throws SessionException is the session does not exists
	 */
	public void setLastAnswer(String token, Answer answer) throws SessionException {
		Session session = getSession(token);
		session.getAnswers().add(answer);
		session.setLastAnswerTime(LocalDateTime.now());
		repository.save(session);
	}

	/**
	 * @param data {@link SurveyData} for a valid {@link Session}
	 * @return the number of seconds still available to complete the survey of the session
	 * @throws SessionException is the session does not exists
	 */
	public Long getRemainingTime(SurveyData data) throws SessionException {
		Session session = getSession(data.getToken());

		Long seconds = session.getSurvey().getDuration();
		LocalDateTime start = session.getStartTime();
		LocalDateTime end = start.plusSeconds(seconds);

		return start.until(end, ChronoUnit.SECONDS);
	}

	public Integer remainingQuestions(String token) throws SessionException {
		Session session = getSession(token);

		// TODO: check that the returned values are correct
		int questionsDone = session.getAnswers().size();
		int questionTotal = session.getSurvey().getQuestions().size();

		return questionTotal - questionsDone;
	}
}
