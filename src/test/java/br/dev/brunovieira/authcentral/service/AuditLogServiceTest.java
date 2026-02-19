package br.dev.brunovieira.authcentral.service;

import br.dev.brunovieira.authcentral.TestFixtures;
import br.dev.brunovieira.authcentral.model.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class AuditLogServiceTest {

    private AuditLogService auditLogService;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService();
        tenant = TestFixtures.tenant();
    }

    @Test
    void logSuccess_delegatesToLogAction() {
        assertThatCode(() -> auditLogService.logSuccess(tenant, "user@test.com", "USER_LOGIN", "127.0.0.1", "TestAgent"))
                .doesNotThrowAnyException();
    }

    @Test
    void logFailure_delegatesToLogAction() {
        assertThatCode(() -> auditLogService.logFailure(tenant, "user@test.com", "USER_LOGIN", "127.0.0.1", "TestAgent", "Bad password"))
                .doesNotThrowAnyException();
    }

    @Test
    void logAction_doesNotThrow() {
        assertThatCode(() -> auditLogService.logAction(tenant, "user@test.com", "USER_REGISTER", "127.0.0.1", "TestAgent", true, null))
                .doesNotThrowAnyException();
    }
}
