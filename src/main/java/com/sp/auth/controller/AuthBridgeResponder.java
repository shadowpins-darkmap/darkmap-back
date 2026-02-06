package com.sp.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sp.auth.dto.response.AuthResponse;
import com.sp.config.EnvironmentConfig;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthBridgeResponder {

    private static final String SUCCESS_TYPE = "OAUTH_SUCCESS";
    private static final String ERROR_TYPE = "OAUTH_ERROR";

    private final ObjectMapper objectMapper;

    public void writeSuccess(HttpServletResponse response,
                             EnvironmentConfig environmentConfig,
                             AuthResponse authResponse) throws IOException {
        Map<String, Object> message = Map.of(
                "type", SUCCESS_TYPE,
                "payload", authResponse
        );
        writeBridge(response, environmentConfig, message);
    }

    public void writeError(HttpServletResponse response,
                           EnvironmentConfig environmentConfig,
                           String errorCode) throws IOException {
        Map<String, Object> message = Map.of(
                "type", ERROR_TYPE,
                "payload", Map.of("error", errorCode)
        );
        writeBridge(response, environmentConfig, message);
    }

    private void writeBridge(HttpServletResponse response,
                             EnvironmentConfig environmentConfig,
                             Map<String, Object> message) throws IOException {
        response.setStatus(HttpStatus.OK.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.TEXT_HTML_VALUE);

        String messageJson = objectMapper.writeValueAsString(message);
        String targetOrigin = objectMapper.writeValueAsString(environmentConfig.getFrontendUrl());

        String html = """
                <!DOCTYPE html>
                <html lang="ko">
                <head>
                    <meta charset="UTF-8" />
                    <title>Authentication Completed</title>
                    <style>
                        body { font-family: sans-serif; background: #0d0d0d; color: #f5f5f5; display: flex; align-items: center; justify-content: center; height: 100vh; margin: 0; }
                        .card { text-align: center; padding: 24px; border: 1px solid rgba(255,255,255,0.1); border-radius: 8px; background: rgba(255,255,255,0.05); }
                        .card h1 { font-size: 1.25rem; margin-bottom: 12px; }
                        .card p { margin: 0; font-size: 0.95rem; color: rgba(255,255,255,0.8); }
                    </style>
                </head>
                <body>
                <div class="card">
                    <h1>Processing sign-in…</h1>
                    <p>이 창은 곧 자동으로 닫힙니다.</p>
                </div>
                <script>
                    (function() {
                        const message = %s;
                        const targetOrigin = %s;
                        const fallbackUrl = targetOrigin + "/login";
                        const successSuffix = message.type === 'OAUTH_SUCCESS' ? '?success=true' : '?success=false';
                        const hashPayload = encodeURIComponent(JSON.stringify(message));

                        if (window.opener && !window.opener.closed) {
                            window.opener.postMessage(message, targetOrigin);
                            window.close();
                            return;
                        }

                        window.location.replace(fallbackUrl + successSuffix + "#auth_payload=" + hashPayload);
                    })();
                </script>
                </body>
                </html>
                """.formatted(messageJson, targetOrigin);

        response.getWriter().write(html);
        response.getWriter().flush();
    }
}
