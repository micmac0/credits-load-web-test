package org.credits.load.loadtest.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration

public class ThreadConfig {

	@Bean

	public TaskExecutor threadPoolTaskExecutor() {

		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

		executor.setCorePoolSize(200);

		executor.setMaxPoolSize(400);

		executor.setThreadNamePrefix("default_task_executor_thread");

		executor.initialize();

		return executor;

	}

}