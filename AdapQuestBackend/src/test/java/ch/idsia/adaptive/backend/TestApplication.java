package ch.idsia.adaptive.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.metrics.ApplicationStartup;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: AdapQuest
 * Date:    19.01.2021 13:53
 */
@SpringBootApplication
@ComponentScan(excludeFilters = @ComponentScan.Filter(
		type = FilterType.ASSIGNABLE_TYPE,
		value = {ApplicationStartup.class, AdapQuestBackend.class}
))
public class TestApplication {
	public static void main(String[] args) {
		System.setProperty("server.servlet.context-path", "/");
		SpringApplication.run(AdapQuestBackend.class, args);
	}
}
