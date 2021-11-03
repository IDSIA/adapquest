package ch.idsia.adaptive.backend.services.templates;

import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    03.11.2021 08:45
 */
public class TemplateJSON {
	private static final Logger logger = LoggerFactory.getLogger(TemplateJSON.class);

	public static ImportStructure parse(Path path) throws IOException {
		final ObjectMapper om = new ObjectMapper();

		logger.info("Reading template JSON file={}", path.toFile());
		return om.readValue(path.toFile(), ImportStructure.class);
	}

}
