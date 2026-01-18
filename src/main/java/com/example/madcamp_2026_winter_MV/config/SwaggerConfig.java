package com.example.madcamp_2026_winter_MV.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        Server localServer = new Server().url("http://localhost:8080").description("로컬 서버");
        Server prodServer = new Server().url("http://madcamp-view.com").description("배포 서버");

        return new OpenAPI()
                .info(new Info()
                        .title("MadCamp view API 명세서")
                        .description("게시글, 채팅, 멤버 관리 API 명세서")
                        .version("1.0.0"))
                .servers(List.of(prodServer, localServer));
    }
}