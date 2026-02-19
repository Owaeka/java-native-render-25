package br.dev.brunovieira.authcentral.config;

import br.dev.brunovieira.authcentral.model.Tenant;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
@Slf4j
public class TenantProperties {

    private List<TenantEntry> tenants = new ArrayList<>();

    private Map<String, Tenant> tenantMap;

    @PostConstruct
    void buildLookup() {
        AtomicLong idSeq = new AtomicLong(1);
        tenantMap = tenants.stream()
                .filter(e -> e.getKey() != null && !e.getKey().isBlank())
                .map(e -> Tenant.builder()
                        .id(idSeq.getAndIncrement())
                        .tenantKey(e.getKey())
                        .tenantName(e.getName())
                        .realmName(e.getRealmName())
                        .clientId(e.getClientId())
                        .clientSecret(e.getClientSecret())
                        .keycloakBaseUrl(e.getKeycloakBaseUrl())
                        .isActive(true)
                        .build())
                .collect(Collectors.toMap(Tenant::getTenantKey, t -> t));

        log.info("Loaded {} tenant(s) from configuration: {}", tenantMap.size(), tenantMap.keySet());
    }

    public Optional<Tenant> findByKey(String key) {
        return Optional.ofNullable(tenantMap.get(key));
    }

    @Getter
    @Setter
    public static class TenantEntry {
        private String key;
        private String name;
        private String realmName;
        private String clientId;
        private String clientSecret;
        private String keycloakBaseUrl;
    }
}
