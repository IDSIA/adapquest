package ch.idsia.adaptive.backend.services;

import ch.idsia.adaptive.backend.persistence.dao.ExperimentRepository;
import ch.idsia.adaptive.backend.persistence.dao.SurveyRepository;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileInputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    29.11.2021 14:24
 */
@ExtendWith(MockitoExtension.class)
public class TestExperimentService {

	@Mock
	SurveyRepository sr;
	@Mock
	ExperimentRepository er;

	ExperimentService es;

	@BeforeEach
	void setup() {
		es = new ExperimentService(new InitializationService(sr), er);
	}

	@Disabled // TODO: make this a real test
	@Test
	public void testExperiment() {
		es.exec("adapquest.small.xlsx");
	}

}