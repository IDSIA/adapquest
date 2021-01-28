package ch.idsia.adaptive.experiments;


import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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
	void testConnectionUsingApiKey() throws Exception {

		HttpRequest request = HttpRequest.newBuilder()
				.uri(new URI("http://" + host + ":" + port + "/console/add/survey/"))
				.header("APIKey", APIKey)
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();

		HttpResponse<String> response = HttpClient.newBuilder()
				.build()
				.send(request, HttpResponse.BodyHandlers.ofString());

		System.out.println(response.body());
	}
}
