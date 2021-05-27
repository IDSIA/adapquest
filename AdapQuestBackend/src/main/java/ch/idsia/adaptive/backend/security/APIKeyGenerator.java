package ch.idsia.adaptive.backend.security;

import ch.idsia.adaptive.backend.persistence.model.Client;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    29.01.2021 09:43
 */
public class APIKeyGenerator {

	public static String validateApiKey(String magic, String key) throws Exception {
		MessageDigest md = MessageDigest.getInstance("SHA-512");
		md.update(magic.getBytes(StandardCharsets.UTF_8));
		byte[] bytes = md.digest(key.getBytes(StandardCharsets.UTF_8));
		StringBuilder sb = new StringBuilder();
		for (byte b : bytes) {
			sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
		}
		return sb.toString();
	}

	public static String generateApiKey(String magic, Client c) throws Exception {
		String key = UUID.randomUUID().toString();
		try {
			String hashed = validateApiKey(magic, key);
			c.setKey(hashed);
		} catch (NoSuchAlgorithmException e) {
			throw new Exception("Could not secure api key!", e);
		}
		return key;
	}
}
