package br.dev.brunovieira.authcentral.service;

import br.dev.brunovieira.authcentral.model.Tenant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class AuditLogService {

    public void logAction(Tenant tenant, String userEmail, String action, String ipAddress,
                          String userAgent, boolean success, String errorMessage) {
        log.info("AUDIT action={} user={} tenant={} ip={} success={} error={}",
                action, userEmail, tenant.getTenantKey(), ipAddress, success, errorMessage);
    }

    public void logSuccess(Tenant tenant, String userEmail, String action, String ipAddress, String userAgent) {
        logAction(tenant, userEmail, action, ipAddress, userAgent, true, null);
    }

    public void logFailure(Tenant tenant, String userEmail, String action, String ipAddress,
                           String userAgent, String errorMessage) {
        logAction(tenant, userEmail, action, ipAddress, userAgent, false, errorMessage);
    }
}
