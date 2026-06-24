package com.bocfintech.allstar.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务配置
 * <p>
 * 核心策略：邮件发送使用单线程串行执行，避免多个 Chrome 进程同时启动
 * 导致 CDP 端口冲突 / User Data Dir 锁冲突 / 系统资源耗尽。
 * <p>
 * 线程池参数：
 * <pre>
 *   corePoolSize  = 1   ← 只有1个核心线程，保证串行
 *   maxPoolSize   = 2   ← 队列满时最多扩到2（极端情况兜底）
 *   queueCapacity = 100 ← 最多排队100个邮件任务
 * </pre>
 */
@Configuration
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

    /**
     * 邮件发送专用线程池 — 单线程串行执行
     */
    @Bean(name = "mailTaskExecutor")
    public Executor mailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("mail-async-");
        // 队列满时的拒绝策略：由调用线程执行（不丢任务）
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("邮件异步线程池已初始化: core=1, max=2, queue=100");
        return executor;
    }

    /**
     * 全局默认异步执行器（覆盖 SimpleAsyncTaskExecutor）
     * <p>
     * 项目中其他 @Async 方法也会使用这个线程池，避免无限创建线程。
     */
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        log.info("全局异步线程池已初始化: core=2, max=5, queue=200");
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, obj) -> {
            log.error("异步任务执行异常 method={}, params={}", method.getName(), obj, throwable);
        };
    }
}
