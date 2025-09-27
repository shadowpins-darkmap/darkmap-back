package com.sp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.Components;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.base-url:http://localhost:8080}")
    private String serverBaseUrl;

    @Bean
    public OpenAPI customOpenAPI() {
        List<Server> servers = new ArrayList<>();

        // 환경변수나 프로퍼티에서 가져온 서버 URL 추가
        servers.add(new Server()
                .url(serverBaseUrl)
                .description("Current Server"));

        // 개발용 로컬 서버는 항상 포함
        if (!serverBaseUrl.equals("http://localhost:8080")) {
            servers.add(new Server()
                    .url("http://localhost:8080")
                    .description("Local Development Server"));
        }

        return new OpenAPI()
                .info(new Info()
                        .title("Community Board API")
                        .description("커뮤니티 게시판 API 문서")
                        .version("1.0.0")
                        .license(new License()
                                .name("Apache 2.0")
                                .url("http://springdoc.org")))
                .servers(servers)
                .components(new Components()
                        .addSecuritySchemes("bearerAuth",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT 토큰을 입력하세요")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));
    }
}