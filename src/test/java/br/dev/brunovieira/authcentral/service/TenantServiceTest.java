package br.dev.brunovieira.authcentral.service;

import br.dev.brunovieira.authcentral.TestFixtures;
import br.dev.brunovieira.authcentral.config.TenantProperties;
import br.dev.brunovieira.authcentral.exception.TenantNotFoundException;
import br.dev.brunovieira.authcentral.model.Tenant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantProperties tenantProperties;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void getTenantByKey_activeFound() {
        Tenant tenant = TestFixtures.tenant();
        when(tenantProperties.findByKey("test-tenant")).thenReturn(Optional.of(tenant));

        Tenant result = tenantService.getTenantByKey("test-tenant");

        assertThat(result).isEqualTo(tenant);
    }

    @Test
    void getTenantByKey_inactive() {
        Tenant tenant = TestFixtures.tenant();
        tenant.setIsActive(false);
        when(tenantProperties.findByKey("test-tenant")).thenReturn(Optional.of(tenant));

        assertThatThrownBy(() -> tenantService.getTenantByKey("test-tenant"))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void getTenantByKey_notFound() {
        when(tenantProperties.findByKey("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tenantService.getTenantByKey("unknown"))
                .isInstanceOf(TenantNotFoundException.class);
    }
}
