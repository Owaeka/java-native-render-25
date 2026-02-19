package br.dev.brunovieira.authcentral.interceptor;

import br.dev.brunovieira.authcentral.TestFixtures;
import br.dev.brunovieira.authcentral.exception.TenantNotFoundException;
import br.dev.brunovieira.authcentral.model.Tenant;
import br.dev.brunovieira.authcentral.service.TenantService;
import br.dev.brunovieira.authcentral.util.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantInterceptorTest {

    @Mock
    private TenantService tenantService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private TenantInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new TenantInterceptor(tenantService);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void preHandle_success() {
        Tenant tenant = TestFixtures.tenant();
        when(request.getHeader("X-Tenant-Key")).thenReturn("test-tenant");
        when(tenantService.getTenantByKey("test-tenant")).thenReturn(tenant);

        boolean result = interceptor.preHandle(request, response, new Object());

        assertThat(result).isTrue();
        assertThat(TenantContext.getCurrentTenant()).isEqualTo(tenant);
    }

    @Test
    void preHandle_missingHeader() {
        when(request.getHeader("X-Tenant-Key")).thenReturn(null);
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessageContaining("X-Tenant-Key header is required");
    }

    @Test
    void preHandle_emptyHeader() {
        when(request.getHeader("X-Tenant-Key")).thenReturn("");
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessageContaining("X-Tenant-Key header is required");
    }

    @Test
    void preHandle_tenantNotFound() {
        when(request.getHeader("X-Tenant-Key")).thenReturn("unknown-tenant");
        when(tenantService.getTenantByKey("unknown-tenant"))
                .thenThrow(new TenantNotFoundException("Tenant not found"));

        assertThatThrownBy(() -> interceptor.preHandle(request, response, new Object()))
                .isInstanceOf(TenantNotFoundException.class);
    }

    @Test
    void afterCompletion_clearsTenantContext() {
        TenantContext.setCurrentTenant(TestFixtures.tenant());
        assertThat(TenantContext.getCurrentTenant()).isNotNull();

        interceptor.afterCompletion(request, response, new Object(), null);

        assertThat(TenantContext.getCurrentTenant()).isNull();
    }
}
