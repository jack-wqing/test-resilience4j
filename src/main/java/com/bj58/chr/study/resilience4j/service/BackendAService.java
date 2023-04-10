package com.bj58.chr.study.resilience4j.service;


import com.bj58.chr.study.resilience4j.exception.BusinessException;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

import static io.github.resilience4j.bulkhead.annotation.Bulkhead.Type;

/**
 * This Service shows how to use the CircuitBreaker annotation.
 */
@Component(value = "backendAService")
public class BackendAService implements Service {

    @Resource
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @Resource
    private BulkheadRegistry bulkheadRegistry;

    @PostConstruct
    public void init(){
        CircuitBreakerRegistry tempCBR = this.circuitBreakerRegistry;
        BulkheadRegistry tempBR = this.bulkheadRegistry;
    }

    @Override
    @CircuitBreaker(name = "#root.methodName", fallbackMethod = "fallback")
    @Bulkhead(name = "#root.methodName")
    public String failure() {
        throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "This is a remote exception");
    }

    @Override
    @CircuitBreaker(name = "#root.methodName")
    @Bulkhead(name = "#root.methodName")
    public String ignoreException() {
        throw new BusinessException("This exception is ignored by the CircuitBreaker of backend A");
    }

    @Override
    @CircuitBreaker(name = "#root.methodName")
    @Bulkhead(name = "#root.methodName")
    public String success() {
        return "Hello World from backend A";
    }

    @Override
    @CircuitBreaker(name = "#root.methodName")
    @Bulkhead(name = "#root.methodName")
    public String successException() {
        throw new HttpClientErrorException(HttpStatus.BAD_REQUEST, "This is a remote client exception");
    }

    @Override
    @CircuitBreaker(name = "#root.methodName", fallbackMethod = "fallback")
    @Bulkhead(name = "#root.methodName")
    public String failureWithFallback() {
        return failure();
    }

    @Override
    @Bulkhead(name = "#root.methodName", type = Type.THREADPOOL)
    @CircuitBreaker(name = "#root.methodName")
    public CompletableFuture<String> futureSuccess() {
        return CompletableFuture.completedFuture("Hello World from backend A");
    }

    @Override
    @Bulkhead(name = "#root.methodName", type = Type.THREADPOOL)
    @CircuitBreaker(name = "#root.methodName")
    public CompletableFuture<String> futureFailure() {
        CompletableFuture<String> future = new CompletableFuture<>();
        future.completeExceptionally(new IOException("BAM!"));
        return future;
    }

    @Override
    @Bulkhead(name = "#root.methodName", type = Type.THREADPOOL)
    @CircuitBreaker(name = "#root.methodName", fallbackMethod = "futureFallback")
    public CompletableFuture<String> futureTimeout() {
        return CompletableFuture.completedFuture("Hello World from backend A");
    }

    private String fallback(HttpServerErrorException ex) {
        return "Recovered HttpServerErrorException: " + ex.getMessage();
    }

    private String fallback(Exception ex) {
        return "Recovered: " + ex.toString();
    }

    private CompletableFuture<String> futureFallback(TimeoutException ex) {
        return CompletableFuture.completedFuture("Recovered specific TimeoutException: " + ex.toString());
    }

    private CompletableFuture<String> futureFallback(BulkheadFullException ex) {
        return CompletableFuture.completedFuture("Recovered specific BulkheadFullException: " + ex.toString());
    }

    private CompletableFuture<String> futureFallback(CallNotPermittedException ex) {
        return CompletableFuture.completedFuture("Recovered specific CallNotPermittedException: " + ex.toString());
    }
}
