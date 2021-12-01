package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    29.11.2021 14:24
 */
@ExtendWith(MockitoExtension.class)
public class TestExperimentService {

	@Mock
	SurveyRepository sr;

	ExperimentService es;

	@BeforeEach
	void setup() {
		es = new ExperimentService(new InitializationService(sr));
	}

	@Test
	public void testExperiment() {
		es.exec("adapquest.small.xlsx");
	}

}