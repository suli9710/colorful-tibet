package com.tibet.tourism.common.config;
import com.tibet.tourism.modules.recommendation.application.ItemBasedRecommendationService;
import com.tibet.tourism.modules.spot.application.PriceUpdateService;
import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 异步任务配置
 * 配置专用线程池处理重量级异步操作（如推荐算法、定时任务等）
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 推荐算法专用线程池
     * 用于 ItemBasedRecommendationService 的矩阵预计算等重量级操作
     */
    @Bean(name = "recommendationExecutor")
    public Executor recommendationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("rec-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 价格更新专用线程池
     * 用于 PriceUpdateService 的批量并行更新
     */
    @Bean(name = "priceUpdateExecutor")
    public Executor priceUpdateExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("price-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(120);
        executor.initialize();
        return executor;
    }

    /**
     * AI 路线生成 / SSE 中继专用线程池。
     * 这些任务会长时间阻塞（生成最长约 185s，跨实例 SSE 中继最长 30min）。它们必须有自己的有界线程池，
     * 且使用 AbortPolicy（而非 CallerRunsPolicy）：池满时直接拒绝（调用方据此返回可重试的 503 / 优雅降级），
     * 绝不能回退到调用线程上执行，否则会占满 Tomcat 请求线程导致整个 API 饥饿。
     */
    @Bean(name = "aiGenerationExecutor")
    public Executor aiGenerationExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(16);
        executor.setThreadNamePrefix("ai-gen-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }

    /**
     * 通用异步任务线程池
     * 用于一般性的异步操作
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("async-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }
}
