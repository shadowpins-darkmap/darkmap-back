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
                            ## Shadow Pins 커뮤니티 API 문서
                            
                            ### ⚠️ 회원 전용 커뮤니티
                            
                            본 서비스는 **회원 전용 커뮤니티**입니다.
                            - 게시글 조회를 포함한 모든 기능은 로그인이 필요합니다.
                            - 비회원은 접근할 수 없습니다.
                            
                            ### 인증 방식
                            
                            #### 1. 소셜 로그인 (카카오/구글)
                            - 브라우저에서 로그인 URL로 직접 접근
                            - 성공 시 프론트엔드로 리다이렉트 (Access Token 포함)
                            - Refresh Token은 HttpOnly Cookie로 자동 설정
                            
                            #### 2. API 인증
                            - **모든 API 호출에 Access Token 필수**
                            - 형식: `Bearer {access_token}`
                            - 유효기간: 30분
                            
                            #### 3. 토큰 갱신
                            - Access Token 만료 시 `/api/v1/auth/refresh` 호출
                            - Refresh Token은 Cookie로 자동 전송 (유효기간: 7일)
                            
                            ### Swagger UI 사용 방법
                            
                            1. **로그인**: 브라우저 새 탭에서 `/api/v1/auth/login/kakao` 접근
                            2. **토큰 복사**: 리다이렉트 URL에서 token 파라미터 복사
                            3. **인증 설정**: 우측 상단 "Authorize 🔓" 버튼 클릭 후 토큰 입력
                            4. **API 테스트**: 모든 게시판 API 테스트 가능
                            
                            ### 인증 필요 여부
                            
                            - 🔓 **인증 불필요**: 로그인, 토큰 갱신
                            - 🔒 **인증 필수**: 게시판 전체, 회원 정보, 로그아웃 등
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
                                        .description("JWT Access Token을 입력하세요. 'Bearer ' 접두사는 자동으로 추가됩니다.")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}