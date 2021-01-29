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
import java.util.List;

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
		HttpPost post = new HttpPost(new URI("http://" + host + ":" + port + "/console/key"));
		List<NameValuePair> params = List.of(
				new BasicNameValuePair("username", "cb"),
				new BasicNameValuePair("email", "claudio@idsia.ch")
		);
		post.setEntity(new UrlEncodedFormEntity(params));
		post.setHeader("APIKey", APIKey);

		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			final CloseableHttpResponse response = httpClient.execute(post);

			System.out.println(EntityUtils.toString(response.getEntity()));
		}
	}
}
