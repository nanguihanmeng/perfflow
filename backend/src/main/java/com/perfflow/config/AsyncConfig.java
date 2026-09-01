package com.perfflow.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
//
 // 异步任务线程池配置。
 //

 //
@Configuration
public class AsyncConfig {

    //
    private static final int CORE_POOL_SIZE = 4;
    //
    private static final int MAX_POOL_SIZE = 8;
    //
    private static final int QUEUE_CAPACITY = 100;
    //
    private static final int KEEP_ALIVE_SECONDS = 60;
    //
     // 异步执行器，线程名带 perfflow-async 前缀便于排查。
     //

     //
    @Bean("asyncExecutor")
    public Executor asyncExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(CORE_POOL_SIZE);
        executor.setMaxPoolSize(MAX_POOL_SIZE);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("perfflow-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.initialize();
        return executor;
    }
}
