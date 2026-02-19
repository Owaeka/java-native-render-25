package br.dev.brunovieira.authcentral.service;

import br.dev.brunovieira.authcentral.config.TenantProperties;
import br.dev.brunovieira.authcentral.exception.TenantNotFoundException;
import br.dev.brunovieira.authcentral.model.Tenant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantProperties tenantProperties;

    @Cacheable(value = "tenants", key = "#tenantKey")
    public Tenant getTenantByKey(String tenantKey) {
        log.debug("Fetching tenant by key: {}", tenantKey);
        return tenantProperties.findByKey(tenantKey)
                .filter(t -> Boolean.TRUE.equals(t.getIsActive()))
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found or inactive: " + tenantKey));
    }
}
