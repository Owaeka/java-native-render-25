package br.dev.brunovieira.authcentral.interceptor;

import br.dev.brunovieira.authcentral.exception.TenantNotFoundException;
import br.dev.brunovieira.authcentral.model.Tenant;
import br.dev.brunovieira.authcentral.service.TenantService;
import br.dev.brunovieira.authcentral.util.RequestUtils;
import br.dev.brunovieira.authcentral.util.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
@Slf4j
public class TenantInterceptor implements HandlerInterceptor {

    private final TenantService tenantService;

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull Object handler) {
        String tenantKey = RequestUtils.getTenantKey(request);

        if (tenantKey == null || tenantKey.isEmpty()) {
            log.warn("Missing X-Tenant-Key header in request to {}", request.getRequestURI());
            throw new TenantNotFoundException("X-Tenant-Key header is required");
        }

        try {
            Tenant tenant = tenantService.getTenantByKey(tenantKey);
            TenantContext.setCurrentTenant(tenant);
            log.debug("Tenant {} resolved for request", tenant.getTenantName());
            return true;
        } catch (TenantNotFoundException e) {
            log.error("Invalid tenant key: {}", tenantKey);
            throw e;
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                @NonNull Object handler, Exception ex) {
        TenantContext.clear();
    }
}
