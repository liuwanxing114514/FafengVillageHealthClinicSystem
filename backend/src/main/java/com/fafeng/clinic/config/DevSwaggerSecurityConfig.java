package com.fafeng.clinic.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;

/**
 * 开发环境放行 Swagger UI 与 OpenAPI JSON，生产环境不加载此配置。
 */
@Configuration
@Profile("dev")
public class DevSwaggerSecurityConfig {

    @Bean
    WebSecurityCustomizer swaggerWebSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/v3/api-docs/**");
    }
}
