package com.client.productionreview.config;

import org.springframework.boot.task.ThreadPoolTaskExecutorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/** Executor próprio para os e-mails de notificação (o envio nunca segura a requisição). */
@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String MAIL_EXECUTOR = "notificationMailExecutor";

    @Bean(name = MAIL_EXECUTOR)
    public ThreadPoolTaskExecutor notificationMailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("mail-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        // fila cheia: descarta o e-mail mais antigo em vez de bloquear quem publicou
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(10);
        return executor;
    }

    /**
     * Um bean Executor próprio faz o Spring Boot deixar de criar o {@code applicationTaskExecutor}
     * (usado pelo MVC assíncrono, ex.: SSE); recriado aqui com a configuração padrão.
     */
    @Lazy
    @Bean(name = {"applicationTaskExecutor", "taskExecutor"})
    public ThreadPoolTaskExecutor applicationTaskExecutor(ThreadPoolTaskExecutorBuilder builder) {
        return builder.build();
    }
}
