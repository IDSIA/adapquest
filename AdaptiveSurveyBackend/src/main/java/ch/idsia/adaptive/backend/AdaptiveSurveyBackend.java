package ch.idsia.adaptive.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdaptiveSurvey
 * Date:    24.11.2020 13:33
 */
@SpringBootApplication
public class AdaptiveSurveyBackend {

	public static void main(String[] args) {
		System.setProperty("server.servlet.context-path", "/");
		SpringApplication.run(AdaptiveSurveyBackend.class, args);
	}

}
