package ch.idsia.adaptive.backend.persistence.model;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    25.11.2020 16:50
 */
public class SurveyToken {

	public static String GUID() {
		return UUID.randomUUID().toString();
	}

	public static String create(SurveyData data) {
		String accessCode = data.getAccessCode();
		String userAgent = data.getUserAgent();
		String remoteAddress = data.getRemoteAddress();

		// TODO: replace this with JWT?
		String str = accessCode + ":" + userAgent + ":" + remoteAddress + ":" + GUID();
		byte[] input = str.getBytes(StandardCharsets.UTF_8);
		return Base64.getEncoder().encodeToString(input);
	}

}
