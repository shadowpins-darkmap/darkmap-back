package com.sp.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

@Slf4j
public class AsyncRetryUtil {

    /**
     * 비동기 작업을 재시도와 함께 실행
     *
     * @param taskName 작업 이름 (로깅용)
     * @param task 실행할 작업
     * @param maxRetries 최대 재시도 횟수
     */
    public static CompletableFuture<Void> executeWithRetry(
            String taskName,
            Runnable task,
            int maxRetries
    ) {
        return CompletableFuture.runAsync(() -> {
            int attempt = 0;
            Exception lastException = null;

            while (attempt <= maxRetries) {
                try {
                    task.run();
                    if (attempt > 0) {
                        log.info("✅ {} 재시도 성공 ({}번째 시도)", taskName, attempt + 1);
                    }
                    return;
                } catch (Exception e) {
                    lastException = e;
                    attempt++;

                    if (attempt <= maxRetries) {
                        long delayMs = (long) Math.pow(2, attempt - 1) * 1000; // 1s, 2s, 4s
                        log.warn("⚠️ {} 실패 ({}번째 시도), {}ms 후 재시도...",
                                taskName, attempt, delayMs);

                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("❌ {} 재시도 중단됨", taskName);
                            return;
                        }
                    }
                }
            }

            // 모든 재시도 실패
            log.error("❌ {} 최종 실패 ({}번 시도 후)", taskName, maxRetries + 1, lastException);
        });
    }

    /**
     * 값을 반환하는 비동기 작업을 재시도와 함께 실행
     */
    public static <T> CompletableFuture<T> executeWithRetry(
            String taskName,
            Supplier<T> task,
            int maxRetries
    ) {
        return CompletableFuture.supplyAsync(() -> {
            int attempt = 0;
            Exception lastException = null;

            while (attempt <= maxRetries) {
                try {
                    T result = task.get();
                    if (attempt > 0) {
                        log.info("✅ {} 재시도 성공 ({}번째 시도)", taskName, attempt + 1);
                    }
                    return result;
                } catch (Exception e) {
                    lastException = e;
                    attempt++;

                    if (attempt <= maxRetries) {
                        long delayMs = (long) Math.pow(2, attempt - 1) * 1000;
                        log.warn("⚠️ {} 실패 ({}번째 시도), {}ms 후 재시도...",
                                taskName, attempt, delayMs);

                        try {
                            Thread.sleep(delayMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.error("❌ {} 재시도 중단됨", taskName);
                            return null;
                        }
                    }
                }
            }

            log.error("❌ {} 최종 실패 ({}번 시도 후)", taskName, maxRetries + 1, lastException);
            return null;
        });
    }
}