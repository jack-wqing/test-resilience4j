package com.bj58.chr.study.resilience4j.service;

import java.util.concurrent.CompletableFuture;

public interface Service {
    String failure();

    String failureWithFallback();

    String success();

    String successException();

    String ignoreException();

    CompletableFuture<String> futureSuccess();

    CompletableFuture<String> futureFailure();

    CompletableFuture<String> futureTimeout();

}
