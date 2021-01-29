package ch.idsia.adaptive.experiments;

import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import ch.idsia.adaptive.backend.persistence.requests.RequestAnswer;
import ch.idsia.adaptive.backend.persistence.requests.RequestClient;
import ch.idsia.adaptive.backend.persistence.requests.RequestKey;
import ch.idsia.adaptive.backend.persistence.responses.ResponseData;
import ch.idsia.adaptive.backend.persistence.responses.ResponseQuestion;
import ch.idsia.adaptive.backend.persistence.responses.ResponseState;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static ch.idsia.adaptive.experiments.StatusCodeCheck.is2xxSuccessful;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    29.01.2021 16:50
 */
public class Tool {
	private static final Logger logger = LogManager.getLogger(Tool.class);

	private final ObjectMapper om = new ObjectMapper();

	private final String host;
	private final Integer port;

	private String key = "";

	private final HttpClient httpClient;

	public Tool(String host, Integer port) {
		this.host = host;
		this.port = port;
		httpClient = HttpClient.newBuilder().build();
	}

	public String getKey() {
		return key;
	}

	private URI endpoint(String path) throws URISyntaxException {
		return new URI("http", null, host, port, path, null, null);
	}

	public void newApiKey(String magic) throws Exception {
		logger.info("Requesting new key.");

		HttpRequest post = HttpRequest.newBuilder()
				.uri(endpoint("/console/key"))
				.header("APIKey", magic)
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(
						new RequestClient("cb", "claudio@idsia.ch")
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

	public void deleteCurrentKey() throws Exception {
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

	public void addSurvey(ImportStructure structure) throws Exception {
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
			throw new Exception("Could add new survey.");
		}

		logger.info("New survey added with accessCode={}", structure.survey.accessCode);
	}

	public String init(String accesCode) throws Exception {
		logger.info("initialization new survey");

		HttpRequest get = HttpRequest.newBuilder()
				.uri(endpoint("/survey/init/" + accesCode))
				.GET()
				.build();

		final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());

		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("Could not initialize survey with accessCode {}", accesCode);
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not initialize survey.");
		}

		ResponseData data = om.readValue(response.body(), ResponseData.class);
		logger.info("Survey initialized with token={}", data.token);

		return data.token;
	}

	public ResponseQuestion nextQuestion(String token) throws Exception {
		logger.info("Request new question for token={}", token);

		HttpRequest get = HttpRequest.newBuilder()
				.uri(endpoint("/survey/question/" + token))
				.GET()
				.build();

		// get next question
		final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());

		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("Could not get next question for token={}", token);
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not get next survey.");
		}

		if (response.statusCode() == 204) {
			logger.info("Finished surevy for token={}", token);
			return null;
		}

		final ResponseQuestion rq = om.readValue(response.body(), ResponseQuestion.class);

		logger.info("new question for token={}: id={} difficulty={}", token, rq.id, rq.explanation);

		return rq;
	}

	public void answer(String token, Long questionId, Long answerId) throws Exception {
		logger.info("answering with token={} questionId={} answerId={}", token, questionId, answerId);

		HttpRequest get = HttpRequest.newBuilder()
				.uri(endpoint("/survey/answer/" + token))
				.POST(HttpRequest.BodyPublishers.ofString(om.writeValueAsString(
						new RequestAnswer(questionId, answerId)
				)))
				.build();

		// get next question
		final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());

		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("Could not check answer for token={}", token);
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not check answer.");
		}

		logger.info("Answer registered for token={}", token);
	}

	public ResponseState state(String token) throws Exception {

		HttpRequest get = HttpRequest.newBuilder()
				.uri(endpoint("/survey/state/" + token))
				.GET()
				.build();

		final HttpResponse<String> response = httpClient.send(get, HttpResponse.BodyHandlers.ofString());

		if (!is2xxSuccessful(response.statusCode())) {
			logger.error("Could not get current state fot token={}", token);
			logger.error("Status code: {}", response.statusCode());
			logger.error("Message:     {}", response.body());
			throw new Exception("Could not get next state.");
		}

		return om.readValue(response.body(), ResponseState.class);
	}

}
