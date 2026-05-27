package com.project.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 27/05/2026
 * @Time: 8:49 AM
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI nexAiOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("NexAi — AI E-commerce Assistant API")
                        .description("Multi-language AI assistant for e-commerce with intent-aware routing, RAG pipeline, and conversation memory.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("MFRA Cloud Intelligence")
                                .email("contact@mfra.io"))
                        .license(new License()
                                .name("Private")
                                .url("https://mfra.io")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local"),
                        new Server().url("https://api.nexai.io").description("Production")))
                .tags(List.of(
                        new Tag().name("Chat").description("AI chat endpoints"),
                        new Tag().name("Products").description("Product management"),
                        new Tag().name("Categories").description("Category management"),
                        new Tag().name("Admin").description("Prompt and data management"),
                        new Tag().name("Analytics").description("Token usage and stats")));
    }
}
