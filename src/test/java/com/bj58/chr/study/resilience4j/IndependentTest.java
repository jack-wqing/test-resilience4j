package com.bj58.chr.study.resilience4j;

import com.sun.jndi.rmi.registry.RegistryContext;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;
import io.vavr.CheckedFunction0;
import io.vavr.control.Try;
import org.junit.Test;

import java.rmi.registry.Registry;
import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * resilience4j 独立测试
 */

public class IndependentTest {
    /**
     * 测试 resilience4J 断路器
     */

    @Test
    public void circuitBreaker(){

        // 自定义配置
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofMillis(1000))
                .permittedNumberOfCallsInHalfOpenState(2)
                .slidingWindowSize(2)
                .build();

        // 断路器注册器
        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);

        // 一个名为name的断路器
        CircuitBreaker nameCircuitBreaker = registry.circuitBreaker("name");

        Supplier<String> doSomeThingSupplier = CircuitBreaker.decorateSupplier(nameCircuitBreaker, BackendService::doSomeThing);

        String result = Try.ofSupplier(doSomeThingSupplier).recover(throwable -> "doSomething recover").get();

        System.out.println(result);

    }

    /**
     * 测试 resilience4j 隔离器
     */
    @Test
    public void testBulkhead() throws InterruptedException {

        BulkheadConfig config = BulkheadConfig.custom()
                .maxConcurrentCalls(1)
                .maxWaitDuration(Duration.ofSeconds(1))
                .build();

        BulkheadRegistry registry = BulkheadRegistry.of(config);

        Bulkhead nameBulkhead = registry.bulkhead("name1");

        Supplier<String> supplier = Bulkhead.decorateSupplier(nameBulkhead, BackendService::doSomeThing);

        new Thread(()->{
            String value = Try.ofSupplier(supplier).recover((throwable -> "Timeout find")).get();
            System.out.println("v:" + value);
        }).start();

        new Thread(()->{
            String value1 = Try.ofSupplier(supplier).recover((throwable -> "Timeout find")).get();
            System.out.println("v1:" + value1);
        }).start();

        Thread.sleep(3000);

    }

    /**
     * 测试 Resilience4j 限流器
     */
    @Test
    public void testRateLimit() throws InterruptedException {

        RateLimiterConfig config = RateLimiterConfig.custom()
                .limitRefreshPeriod(Duration.ofSeconds(1))
                .limitForPeriod(1)
                .timeoutDuration(Duration.ofMillis(500))
                .build();

        RateLimiterRegistry registry = RateLimiterRegistry.of(config);

        RateLimiter rateLimiter = registry.rateLimiter("name1");

        Supplier<String> supplier = RateLimiter.decorateSupplier(rateLimiter, BackendService::doSomeThing);

        new Thread(() -> {
            String v = Try.ofSupplier(supplier).recover(throwable -> "RateLimiter is too many").get();
            System.out.println("v:" + v);
        }).start();

        new Thread(() -> {
            String v1 = Try.ofSupplier(supplier).recover(throwable -> "RateLimiter is too many").get();
            System.out.println("v1:" + v1);
        }).start();

        Thread.sleep(3000);

    }

    /**
     * 测试 resilience4j 重试器
     */
    @Test
    public void testRetry(){

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(2)
                .waitDuration(Duration.ofSeconds(1))
                .build();

        RetryRegistry registry = RetryRegistry.of(retryConfig);

        Retry retry = registry.retry("name1");

        Supplier<String> supplier = Retry.decorateSupplier(retry, BackendService::doSomeThingException);

        String value = Try.ofSupplier(supplier).get();

        System.out.println(value);

    }

    /**
     * 测试 Resilience4j 限时器
     */
    @Test
    public void testTimeLimiterRegistry(){

        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofMillis(500))
                .cancelRunningFuture(true)
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);

        TimeLimiter timeLimiter = registry.timeLimiter("name1");

        try {
            String value = timeLimiter.executeFutureSupplier(() -> CompletableFuture.supplyAsync(new ClassSupplier()));
            System.out.println(value);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * 测试 Resilience4j 缓存功能
     */
    public void testCache(){

    }

    static class BackendService {
        public static String doSomeThing(){
            System.out.println("BackendService.doSomeThing()");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "BackendService.doSomeThing()";
        }
        public static String doSomeThingException(){
            System.out.println("BackendService.doSomeThingException()");
            int a = 1/0;
            return "doSomeThingException";
        }
        public static String doSomeThingParam(){
            System.out.println("BackendService.doSomeThingParam()");
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "BackendService.doSomeThingParam()";
        }
    }

    class ClassSupplier implements Supplier<String>{

        @Override
        public String get() {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return "halle";
        }
    }
}
