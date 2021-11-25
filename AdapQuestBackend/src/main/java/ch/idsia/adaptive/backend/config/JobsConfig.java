package ch.idsia.adaptive.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: adapquest
 * Date:    25.11.2021 09:31
 */
@Configuration
@PropertySource("classpath:settings.properties")
@EnableAsync
public class JobsConfig implements AsyncConfigurer {
	private static final Logger logger = LoggerFactory.getLogger(JobsConfig.class);

	@Value("${experiment.pool.size}")
	private Integer poolSize = -1;

	@Override
	public Executor getAsyncExecutor() {
		final int size = poolSize < 0 ? Runtime.getRuntime().availableProcessors() : poolSize;
		logger.debug("Initializing executor with maxPoolSize={}", size);

		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setMaxPoolSize(size);
		executor.initialize();
		return executor;
	}

	@Override
	public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
		return (ex, method, params) -> {
			logger.error("Uncaught exception {} with method: {}", ex.getMessage(), method.getName());
			logger.error("Parameters: {}", String.join(" ", Arrays.toString(params)));
			logger.error("Exception: {}", ex.getMessage(), ex);
		};
	}

}
