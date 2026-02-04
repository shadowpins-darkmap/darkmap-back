package com.sp.api.oauth.handler;

import com.sp.config.EnvironmentConfig;
import com.sp.config.EnvironmentResolver;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private final EnvironmentResolver environmentResolver;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {

        log.error("❌ Google OAuth2 로그인 실패: {}", exception.getMessage());

        EnvironmentConfig envConfig = environmentResolver.resolve(request);
        String redirectUrl = envConfig.getFrontendUrl() +
                "/login?success=false&error=AUTH_FAILED";

        response.sendRedirect(redirectUrl);
    }
}
