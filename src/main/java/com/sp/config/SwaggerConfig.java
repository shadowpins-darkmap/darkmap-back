package com.sp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Shadow Pins API")
                        .description("""
                            ## Shadow Pins API
                            ---
                            
                            ## 🔑 인증 방식
                            
                            ### 쿠키 자동 관리
                            - `access_token`: API 인증용 (30분 유효, HttpOnly)
                            - `refresh_token`: 토큰 갱신용 (7일 유효, HttpOnly)
                            - 모든 요청에 `withCredentials: true` 설정
                            
                            ---
                            
                            ## 🔍 Swagger UI 테스트 방법
                            
                            Swagger UI는 HttpOnly 쿠키를 직접 다룰 수 없습니다.
                            
                            ### 방법 1: 브라우저 로그인 후 테스트
                            1. 브라우저 새 탭: `/api/v1/auth/login/kakao` 접근
                            2. 로그인 완료 (쿠키 자동 설정됨)
                            3. Swagger UI로 돌아와서 API 테스트
                            4. 쿠키가 자동 전송되어 인증됨
                            
                            ### 방법 2: Bearer Token 직접 입력
                            1. 브라우저 개발자 도구 → Application → Cookies
                            2. `access_token` 값 복사
                            3. Swagger "Authorize 🔓" 버튼 클릭
                            4. 복사한 토큰 입력 (Bearer 접두사 제외)
                            
                            """)
                        .version("1.0.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .servers(List.of(
                        new Server()
                                .url("https://api.kdark.weareshadowpins.com")
                                .description("Production Server"),
                        new Server()
                                .url("http://localhost:8080")
                                .description("Local Development Server")
                ))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                            ## 쿠키 우선 인증
                                            
                                            이 API는 쿠키를 우선적으로 사용합니다:
                                            1. `access_token` 쿠키 확인
                                            2. 없으면 Authorization 헤더 확인
                                            
                                            ### 테스트 방법
                                            - 브라우저에서 `/api/v1/auth/login/kakao` 로그인
                                            - 쿠키가 자동 설정되어 인증됨
                                            
                                            ### 대안: Bearer Token 직접 입력
                                            개발/테스트 목적으로만 사용:
                                            1. 개발자 도구에서 `access_token` 쿠키 값 복사
                                            2. 여기에 붙여넣기 (Bearer 접두사 제외)
                                            """)))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}