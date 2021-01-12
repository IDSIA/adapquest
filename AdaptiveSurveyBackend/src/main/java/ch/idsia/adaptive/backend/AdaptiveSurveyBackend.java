package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.services.InitializationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

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

	private final InitializationService initializationService;

	@Autowired
	public AdaptiveSurveyBackend(InitializationService initializationService) {
		this.initializationService = initializationService;
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup() {
		initializationService.init();
	}

}
