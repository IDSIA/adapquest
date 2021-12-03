package ch.idsia.adaptive.backend;

import ch.idsia.adaptive.backend.services.InitializationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    24.11.2020 13:33
 */
@SpringBootApplication
public class AdapQuestBackend {

	public static void main(String[] args) {
		SpringApplication.run(AdapQuestBackend.class, args);
	}

	private final InitializationService initializationService;

	@Autowired
	public AdapQuestBackend(InitializationService initializationService) {
		this.initializationService = initializationService;
	}

	@EventListener(ContextRefreshedEvent.class)
	public void doSomethingAfterStartup() {
		initializationService.init();
	}

}
