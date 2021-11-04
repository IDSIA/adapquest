package ch.idsia.adaptive.experiments;

import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.requests.RequestAnswer;
import ch.idsia.adaptive.backend.persistence.requests.RequestClient;
import ch.idsia.adaptive.backend.persistence.requests.RequestCode;
import ch.idsia.adaptive.backend.persistence.requests.RequestKey;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashSet;
import java.util.Set;

import static ch.idsia.adaptive.experiments.utils.StatusCodeCheck.is2xxSuccessful;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    29.01.2021 16:50
 * <p>
 * This tool is intended to simplify the connection to a remote machine running the AdapQuestBackend.
 */
public class Tool {
	private static final Logger logger = LoggerFactory.getLogger(Tool.class);

	private final ObjectMapper om = new ObjectMapper().registerModule(new JavaTimeModule());

	private final String host;
	private final Integer port;

	private final String username;
	private final String email;

	private final HttpClient httpClient;

	private final Set<String> accessCodes = new HashSet<>();

	/**
	 * Personal connection key.
	 */
	private String key = "";

	/**
	 * Creates an object that can connect to the remote application.
	 *
	 * @param host     name of the remote application to connect to.
	 * @param port     port of the remote application to connect to.
	 * @param username identifier of the person that request a new key.
	 * @param email    contact information of the person that request a new key.
	 */
	public Tool(String host, Integer port, String username, String email) {
		this.host = host;
		this.port = port;
		this.username = username;
		this.email = email;
		httpClient = HttpClient.newBuilder().build();
	}

	/**
	 * @return the current personal connection key.
	 */
	public String getKey() {
		return key;
	}

	/**
	 * Overwrite the API key with a new one.
	 *
	 * @param key the new API key to use.
	 */
	public void setKey(String key) {
		this.key = key;
	}

	/**
	 * Utility, it build an {@link URI} to the correct endpoint.
	 *
	 * @param path endpoint to use
	 * @return the correct URI object to use to connect to a given endpoint.
	 * @throws URISyntaxException If it is not possible to build the URI.
	 */
	private URI endpoint(String path) throws URISyntaxException {
		return new URI("http", null, host, port, path, null, null);
	}

	/**
	 * @return true if the key is valid, otherwise false
	 */
	public boolean isKeyValid() {
		logger.info("Testing current key={}", key);

		try {
			HttpRequest get = HttpRequest.newBuilder()
					.uri(endpoint("/console/key"))
					.header("APIKey", key)
					.header("Content-Type", "application/json")
					.GET()
					.build();

			final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());

			return is2xxSuccessful(response.statusCode());
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Given a valid MAGIC KEY, request a new personal key to the remote application. If successful, a new key will be
	 * stored in the {@link #key} field and can be retrieved with the {@link #getKey()} method.
	 *
	 * @param magic a valid MAGIC KEY to use for the request.
	 * @throws Exception if there are issues with the URI creation, or if the content of the {@link RequestClient} object
	 *                   cannot be correctly written, or when there is an issue with the {@link HttpClient} sending the
	 *                   request, or when the return code is not valid (a 2xx class is expected).
	 */
	public void newApiKey(String magic) throws Exception {
		logger.info("Requesting new key.");

		HttpRequest post = HttpRequest.newBuilder()
				.uri(endpoint("/console/key"))
				.header("APIKey", magic)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(
						new RequestClient(username, email)
				)))
				.build();

		final HttpResponse<String> response = httpClient.send(post, HttpResponse.BodyHandlers.ofString());
		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("Could not get a new key");
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not get a new key.");
		}

		key = response.body();
	}

	/**
	 * Request the invalidation and removal of the current personal key to the remote application. After a call to this
	 * method, the save {@link #key} will be empty and no more request can be made to the remote application using the
	 * same {@link Tool} object.
	 * <p>
	 * If the key is already empty, does nothing.
	 *
	 * @throws Exception if there are issues with the URI creation, or if the content of the {@link RequestKey} object
	 *                   cannot be correctly written, or when there is an issue with the {@link HttpClient} sending the
	 *                   request, or when the return code is not valid (a 2xx class is expected).
	 */
	public void removeKey() throws Exception {
		if (key.isEmpty()) {
			logger.warn("No key saved.");
			return;
		}

		logger.info("Deleting key={}", key);

		HttpRequest delete = HttpRequest.newBuilder()
				.uri(endpoint("/console/key"))
				.header("APIKey", key)
				.header("Content-Type", "application/json")
				.method("DELETE", HttpRequest.BodyPublishers.ofString(om.writeValueAsString(
						new RequestKey(key)
				)))
				.build();

		final HttpResponse<String> response = httpClient.send(delete, HttpResponse.BodyHandlers.ofString());

		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("Could not delete key");
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not delete key.");
		}

		key = "";
		logger.info("Key deleted.");
	}

	/**
	 * Add a new Survey to the remote application by sending the given {@link ImportStructure} object. This object need
	 * to have a valid structure. The access code associated with the givent {@link ImportStructure} is saved internally.
	 *
	 * @param structure structure containing the survey to add. This structure need to have a non null
	 *                  {@link ch.idsia.adaptive.backend.persistence.external.SurveyStructure} and a non empty
	 *                  AccessCode assigned. Other fields are not mandatory but it will be futile to have a survey
	 *                  without a model or questions...
	 * @throws Exception if the given {@link ImportStructure} is invalid, or if the given AccessCode is empty, or if
	 *                   there are issue with the URI creation, or if the content of the {@link ImportStructure} object
	 *                   cannot be correctly written, or when there is an issue with the {@link HttpClient} sending the
	 *                   request, or when the return code is not valid (a 2xx class is expected).
	 */
	public void addSurvey(ImportStructure structure) throws Exception {
		if (structure.survey == null) {
			logger.info("Missing survey object inside ImportStructure object.");
			throw new Exception("Invalid structure: survey is missing!");
		}
		if (structure.survey.getAccessCode().isEmpty()) {
			logger.info("AccessCode is empty for the given SurveyStructure object inside ImportStructure object.");
			throw new Exception("Invalid structure: survey has an empty accessCode!");
		}

		logger.info("adding new survey with accessCode={}", structure.survey.accessCode);

		HttpRequest get = HttpRequest.newBuilder()
				.uri(endpoint("/console/survey"))
				.header("APIKey", key)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(structure)))
				.build();

		// get next question
		final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());

		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("Could not add new survey with accessCode={}", structure.survey.accessCode);
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not add new survey.");
		}

		accessCodes.add(structure.survey.accessCode);
		logger.info("New survey added with accessCode={}", structure.survey.accessCode);
	}

	/**
	 * Remove a survey identified by an accessCode, and everything connect with it (sessions, answers, states).
	 * <p>
	 * If the key is already empty, does nothing.
	 *
	 * @param accessCode a valid access code of a survey to remove
	 * @throws Exception if there are issues with the URI creation, or if the content of the {@link RequestCode} object
	 *                   cannot be correctly written, or when there is an issue with the {@link HttpClient} sending the
	 *                   request, or when the return code is not valid (a 2xx class is expected).
	 */
	public void removeSurvey(String accessCode) throws Exception {
		if (key.isEmpty()) {
			logger.warn("No key saved.");
			return;
		}

		logger.info("Deleting survey with accessCode={}", accessCode);

		HttpRequest delete = HttpRequest.newBuilder()
				.uri(endpoint("/console/survey/"))
				.header("APIKey", key)
				.header("Content-Type", "application/json")
				.method("DELETE", HttpRequest.BodyPublishers.ofString(om.writeValueAsString(
						new RequestCode(accessCode)
				)))
				.build();

		final HttpResponse<String> response = httpClient.send(delete, HttpResponse.BodyHandlers.ofString());

		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("Could not delete survey with code={}", accessCode);
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not delete survey.");
		}

		logger.info("Survey with accessCode={} deleted.", accessCode);
	}

	/**
	 * Removes all surveys added using this tool.
	 *
	 * @throws Exception if some surveys cannot be removed.
	 */
	public void removeAllSurvey() throws Exception {
		for (String accessCode : accessCodes) {
			try {
				removeSurvey(accessCode);
				logger.info("Removed survey with accessCode={}", accessCode);
			} catch (Exception e) {
				logger.error("Could not remove survey with accessCode={}", accessCode);
				throw e;
			}
		}
		accessCodes.clear();
		logger.info("Removed all surveys");
	}

	/**
	 * Checks if the given accessCode has a valid survey associated with.
	 *
	 * @param accessCode a valid access code of a survey to check for its existence.
	 * @return false if the given accessCode does not have a valid survey or the personal key is empty, otherwise true.
	 * @throws Exception if there are issues with the URI creation, or when there is an issue with the
	 *                   {@link HttpClient} sending the request, or when the return code is not valid (a 2xx class is
	 *                   expected).
	 */
	public boolean checkSurvey(String accessCode) throws Exception {
		if (key.isEmpty()) {
			logger.warn("No key saved.");
			return false;
		}

		logger.info("checking if survey with accessCode={} exists", accessCode);

		HttpRequest get = HttpRequest.newBuilder()
				.uri(endpoint("/console/survey/" + accessCode))
				.header("APIKey", key)
				.GET()
				.build();

		final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());
		logger.info("response: {}", response.body());

		return is2xxSuccessful(response.statusCode());
	}

	/**
	 * Initialize a new survey on the remote server, and returns a valid token associated with the current remote
	 * session.
	 *
	 * @param accessCode a valid access code of a survey.
	 * @return a valid token associated with the current remote session.
	 * @throws Exception if there are issues with the URI creation, or when there is an issue with the
	 *                   {@link HttpClient} sending the request, or when the return code is not valid (a 2xx class is
	 *                   expected), or when the returned {@link ResponseData} cannot be correctly read.
	 */
	public String init(String accessCode) throws Exception {
		logger.info("initialization new survey");

		HttpRequest get = HttpRequest.newBuilder()
				.uri(endpoint("/survey/init/" + accessCode))
				.GET()
				.build();

		final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());

		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("Could not initialize survey with accessCode {}", accessCode);
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not initialize survey.");
		}

		ResponseData data = om.readValue(response.body(), ResponseData.class);
		logger.info("token={} survey initialized", data.token);

		return data.token;
	}

	/**
	 * Reqest the remote application for a new question, and return it.
	 *
	 * @param token a valid token created using the {@link #init(String)} method.
	 * @return a {@link ResponseQuestion} object with the next question to answer to, null if the survey is completed.
	 * @throws Exception if there are issues with the URI creation, or when there is an issue with the
	 *                   {@link HttpClient} sending the request, or when the return code is not valid (a 2xx class is
	 *                   expected), or when the returned {@link ResponseQuestion} cannot be correctly read.
	 */
	public ResponseQuestion nextQuestion(String token) throws Exception {
		logger.info("token={} request new question", token);

		HttpRequest get = HttpRequest.newBuilder()
				.uri(endpoint("/survey/question/" + token))
				.GET()
				.build();

		// get next question
		final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());

		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("token={} could not get next question", token);
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not get next survey.");
		}

		if (response.statusCode() == 204) {
			logger.info("token={} finished survey ", token);
			return null;
		}

		final ResponseQuestion rq = om.readValue(response.body(), ResponseQuestion.class);

		logger.info("token={} new question with id={} difficulty={}", token, rq.id, rq.explanation);

		return rq;
	}

	/**
	 * Send the answer to the remote application. The possible answerIds can be found in the {@link ResponseQuestion}
	 * object returned by the {@link #nextQuestion(String)} method.
	 *
	 * @param token      a valid token created using the {@link #init(String)} method.
	 * @param questionId id found of the {@link ResponseQuestion} returned by the {@link #nextQuestion(String)} method.
	 * @param answerId   id of the answer from a  {@link ResponseQuestion} returned by the {@link #nextQuestion(String)}.
	 * @throws Exception if there are issues with the URI creation, or when there is an issue writing the
	 *                   {@link RequestAnswer} object for the request, or when there is an issue with the
	 *                   {@link HttpClient} sending the request, or when the return code is not valid (a 2xx class is
	 *                   expected).
	 */
	public void answer(String token, Long questionId, Long answerId) throws Exception {
		logger.info("token={} answering to questionId={} with answerId={}", token, questionId, answerId);

		HttpRequest get = HttpRequest.newBuilder()
				.uri(endpoint("/survey/answer/" + token))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(
						new RequestAnswer(questionId, answerId)
				)))
				.build();

		// get next question
		final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());

		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("token={} could not check answer", token);
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not check answer.");
		}

		logger.info("token={} answer checked", token);
	}

	/**
	 * Request the remote application to send the current state of the survey associated with the session identified by
	 * the given token. Thi {@link ResponseState} object contains all the information relative to the skill distribution
	 * and score of a survey.
	 *
	 * @param token a valid token created using the {@link #init(String)} method.
	 * @return a valid {@link ResponseState} object.
	 * @throws Exception if there are issues with the URI creation, or when there is an issue with the
	 *                   {@link HttpClient} sending the request, or when the return code is not valid (a 2xx class is
	 *                   expected), or when it is not possible to parse the returned {@link ResponseState} object.
	 */
	public ResponseState state(String token) throws Exception {
		logger.info("token={} requests current state", token);

		HttpRequest get = HttpRequest.newBuilder()
				.uri(endpoint("/survey/state/" + token))
				.GET()
				.build();

		final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());

		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("token={} could not get current state", token);
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not get next state.");
		}

		return om.readValue(response.body(), ResponseState.class);
	}

}
