package br.dev.brunovieira.authcentral.util;

import br.dev.brunovieira.authcentral.TestFixtures;
import br.dev.brunovieira.authcentral.model.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TenantContextTest {

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void setAndGetCurrentTenant() {
        Tenant tenant = TestFixtures.tenant();
        TenantContext.setCurrentTenant(tenant);

        assertThat(TenantContext.getCurrentTenant()).isEqualTo(tenant);
    }

    @Test
    void clear_removesValue() {
        TenantContext.setCurrentTenant(TestFixtures.tenant());
        TenantContext.clear();

        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void getCurrentTenant_defaultNull() {
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
