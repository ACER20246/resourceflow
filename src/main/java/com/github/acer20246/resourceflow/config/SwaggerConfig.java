package com.github.acer20246.resourceflow.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        // 1. 创建一个 Bearer JWT 类型的安全方案
        SecurityScheme bearerScheme = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP) // 类型为 HTTP
                .scheme("bearer")               // 方案为 bearer
                .bearerFormat("JWT");           // 格式为 JWT

        return new OpenAPI()
                // 2. 将安全方案应用到全局（所有接口）
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                // 3. 注册这个安全方案，并给它起个名字（如 "Bearer Authentication"）
                .components(new Components().addSecuritySchemes("Bearer Authentication", bearerScheme))
                .info(new Info().title("My REST API").version("1.0"));
    }
}