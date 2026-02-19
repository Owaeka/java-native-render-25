package br.dev.brunovieira.authcentral.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    private boolean enabled = true;
    private EndpointLimit login;
    private EndpointLimit register;

    @Data
    public static class EndpointLimit {
        private int capacity;
        private int refillTokens;
        private int refillPeriod; // in seconds
    }
}
