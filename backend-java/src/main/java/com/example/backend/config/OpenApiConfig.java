package com.example.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class OpenApiConfig implements WebMvcConfigurer {

    private static final String SECURITY_SCHEME_NAME = "ApiKeyAuth";
    private final AppConfig cfg;

    public OpenApiConfig(AppConfig cfg) {
        this.cfg = cfg;
    }

    @Bean
    public OpenAPI openAPI() {
        String apiKeyHeader = cfg.getApiKeyHeader();

        SecurityScheme apiKeyScheme = new SecurityScheme()
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .name(apiKeyHeader);

        return new OpenAPI()
                .info(new Info()
                        .title("Self-Hosted Enterprise RAG Backend API")
                        .version("0.1.0")
                        .description("Spring Boot API gateway for document ingest, tasks, and RAG query."))
                .schemaRequirement(SECURITY_SCHEME_NAME, apiKeyScheme)
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME));
    }

    /**
     * Your application.yml disables default static resource mappings:
     * spring.web.resources.add-mappings=false
     * Swagger UI needs static assets. We add explicit handlers to avoid changing
     * global settings.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/")
                .resourceChain(false);

        registry.addResourceHandler("/swagger-ui.html")
                .addResourceLocations("classpath:/META-INF/resources/")
                .resourceChain(false);

        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/")
                .resourceChain(false);
    }
}