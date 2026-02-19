package br.dev.brunovieira.authcentral.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "keycloak")
public class KeycloakConfig {

    private String url;
    private Admin admin;
    private Token token;

    @Data
    public static class Admin {
        private String realm;
        private String clientId;
        private String clientSecret;
    }

    @Data
    public static class Token {
        private Integer cacheTtl;
    }
}
