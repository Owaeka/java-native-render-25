package br.dev.brunovieira.authcentral.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Auth Central API")
                        .version("1.0.0")
                        .description("Multi-tenant Keycloak authentication translation layer. " +
                                "This API provides a unified interface for user registration, authentication, " +
                                "and token management across multiple Keycloak realms.")
                        .contact(new Contact()
                                .name("Auth Central Team")
                                .email("support@authcentral.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.authcentral.com")
                                .description("Production Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList("X-Tenant-Key"))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("X-Tenant-Key",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("X-Tenant-Key")
                                        .description("Tenant API key for multi-tenant authentication")));
    }
}
