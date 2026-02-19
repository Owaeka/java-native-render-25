package br.dev.brunovieira.authcentral.util;

import br.dev.brunovieira.authcentral.model.Tenant;

/**
 * Thread-local storage for current tenant
 */
public class TenantContext {

    private static final ThreadLocal<Tenant> currentTenant = new ThreadLocal<>();

    public static void setCurrentTenant(Tenant tenant) {
        currentTenant.set(tenant);
    }

    public static Tenant getCurrentTenant() {
        return currentTenant.get();
    }

    public static void clear() {
        currentTenant.remove();
    }
}
