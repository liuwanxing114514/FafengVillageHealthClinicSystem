package com.fafeng.clinic.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * OpenAPI 文档配置，仅在 dev profile 下生效（prod/docker 通过 springdoc.enabled=false 禁用）。
 */
@Configuration
public class OpenApiConfig {

    @Bean
    @Profile("dev")
    OpenAPI clinicOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("发凤村卫生室诊所辅助系统 API")
                        .description("REST API 文档，仅供本地开发调试；生产环境不可访问 Swagger UI。")
                        .version("2.0.1")
                        .contact(new Contact().name("发凤村卫生室")));
    }
}
