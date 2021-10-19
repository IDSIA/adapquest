package ch.idsia.adaptive.experiments;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    23.02.2021 14:57
 */
public class ToolLocalhost extends Tool {
	private static final Logger logger = LoggerFactory.getLogger(ToolLocalhost.class);

	/**
	 * Creates an object that can connect to the remote application.
	 */
	public ToolLocalhost() {
		super("localhost", 8080, "", "");
	}

	@Override
	public void newApiKey(String magic) throws Exception {
		// requesting a new key to access the remote application using our MAGIC key
		if (magic.isEmpty())
			super.newApiKey("QWRhcHRpdmUgU3VydmV5");
		else
			super.newApiKey(magic);

		logger.info("Key: {}", getKey());
		Files.write(Paths.get(".apikey"), getKey().getBytes()); // key saved to file .apikey
	}
}
