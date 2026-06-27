package dev.skymetron.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI skyMetronOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SkyMetron API")
                        .description("AI Operating System — autonomous agent orchestration, vector vault, ECS, and observability")
                        .version("0.1.0-beta")
                        .contact(new Contact().name("SkyMetron Team").email("dev@skymetron.ai"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local development"),
                        new Server().url("https://api.skymetron.ai").description("Production (placeholder)")));
    }
}
