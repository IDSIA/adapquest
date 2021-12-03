package ch.idsia.adaptive.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
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
@EnableAsync
public class JobsConfig implements AsyncConfigurer {
	private static final Logger logger = LoggerFactory.getLogger(JobsConfig.class);

	private final Integer poolSize;

	private final Environment env;

	@Autowired
	public JobsConfig(Environment env) {
		this.env = env;
		final Integer n = env.getProperty("experiment.pool.size", Integer.class, 1);
		poolSize = n < 0 ? Runtime.getRuntime().availableProcessors() : n;
	}

	@Override
	public Executor getAsyncExecutor() {
		logger.debug("Initializing executor with maxPoolSize={}", poolSize);

		final ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setMaxPoolSize(poolSize);
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
