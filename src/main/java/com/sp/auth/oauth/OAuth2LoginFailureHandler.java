package com.sp.auth.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException exception)
            throws IOException {

        log.warn("❌ OAuth2 로그인 실패: {}", exception.getMessage());

        response.sendRedirect("https://kdark.weareshadowpins.com?error=google");

        // 또는 JSON 응답:
        // response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        // response.setContentType("application/json");
        // response.getWriter().write("{\"error\": \"로그인 실패\"}");
    }
}
