package ch.idsia.adaptive.backend.services.templates;

import ch.idsia.adaptive.backend.persistence.external.ImportStructure;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    03.11.2021 14:01
 */
class TestTemplateXLSX {

	@Test
	public void testLoadFromXLSXTemplate() throws Exception {
		final ImportStructure structure = TemplateXLSX.parse(Paths.get("data", "AdaptiveQuestionnaireTemplate.xlsx"));

		assertFalse(structure.getSkills().isEmpty());
		assertFalse(structure.getQuestions().isEmpty());

		assertEquals(17, structure.getSkills().size());
		assertEquals(18, structure.getQuestions().size());

		assertTrue(structure.survey.getAdaptive());
		assertTrue(structure.survey.getStructural());
		assertFalse(structure.survey.getSimple());

		assertNotNull(structure.modelData);
		assertFalse(structure.modelData.isEmpty());
		assertTrue(structure.modelData.contains("NOISY-OR"));
	}
}