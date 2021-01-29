package ch.idsia.adaptive.experiments;


import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    28.01.2021 16:02
 */
public class Experiments {
	private static final Logger logger = LogManager.getLogger(Experiments.class);

	static final String host = "artemis.idsia.ch";
	static final Integer port = 8080;
	static final String APIKey = "QWRhcHRpdmUgU3VydmV5";

	@Test
	void generateNewApiKey() throws Exception {
		HttpPost post = new HttpPost(new URI("http://" + host + ":" + port + "/console/key"));
		List<NameValuePair> params = List.of(
				new BasicNameValuePair("username", "cb"),
				new BasicNameValuePair("email", "claudio@idsia.ch")
		);
		post.setEntity(new UrlEncodedFormEntity(params));
		post.setHeader("APIKey", APIKey);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			final CloseableHttpResponse response = httpClient.execute(post);

			String key = EntityUtils.toString(response.getEntity());

			logger.info("Key={}", key);

			Files.write(Paths.get(".apikey"), key.getBytes());
		}
	}

	@Test
	void deleteApyKey() throws Exception {
		final String key = new String(Files.readAllBytes(Paths.get(".apikey")));

		logger.info("Deleting key={}", key);

		Map<String, String> data = new HashMap<>();
		data.put("key", key);

		HttpRequest delete = HttpRequest.newBuilder()
				.uri(new URI("http", null, host, port, "/console/key", null, null))
				.header("APIKey", APIKey)
				.header("Content-Type", "multipart/form-data")
				.method("DELETE", buildBodyFromMap(data))
				.build();

		HttpClient httpClient = HttpClient.newBuilder().build();

		final HttpResponse<String> response = httpClient.send(delete, HttpResponse.BodyHandlers.ofString());

		logger.info("response status code: {}", response.statusCode());
		logger.info("content:\n{}", response.body());
	}

	private HttpRequest.BodyPublisher buildBodyFromMap(Map<?, ?> data) {
		final Charset charset = StandardCharsets.UTF_8;
		final String content = data.entrySet().stream()
				.collect(Collectors.toMap(
						entry -> URLEncoder.encode(entry.getKey().toString(), charset),
						entry -> URLEncoder.encode(entry.getValue().toString(), charset)
				))
				.entrySet().stream()
				.map(entry -> entry.getKey() + "=" + entry.getValue())
				.collect(Collectors.joining("&"));

		return HttpRequest.BodyPublishers.ofString(content, charset);
	}
}
