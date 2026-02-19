package br.dev.brunovieira.authcentral;

import br.dev.brunovieira.authcentral.model.Tenant;

public final class TestFixtures {

    private TestFixtures() {
    }

    public static Tenant tenant() {
        return Tenant.builder()
                .id(1L)
                .tenantKey("test-tenant")
                .tenantName("Test Tenant")
                .realmName("test-realm")
                .clientId("test-client-id")
                .clientSecret("test-client-secret")
                .keycloakBaseUrl("http://localhost:8080")
                .isActive(true)
                .build();
    }
}
