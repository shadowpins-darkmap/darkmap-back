package com.sp.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StopWatch;

import java.io.IOException;

/**
 * Lightweight timing interceptor to troubleshoot third-party OAuth latency.
 */
@Slf4j
public class LoggingClientHttpRequestInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try {
            ClientHttpResponse response = execution.execute(request, body);
            stopWatch.stop();
            log.debug("HTTP {} {} -> {} ({} ms)",
                    request.getMethod(),
                    request.getURI(),
                    response.getStatusCode(),
                    stopWatch.getTotalTimeMillis());
            return response;
        } catch (IOException ex) {
            stopWatch.stop();
            log.warn("HTTP {} {} failed after {} ms", request.getMethod(), request.getURI(), stopWatch.getTotalTimeMillis(), ex);
            throw ex;
        }
    }
}
