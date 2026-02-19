package br.dev.brunovieira.authcentral.config;

import br.dev.brunovieira.authcentral.model.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.*;

class NativeHintsRegistrarTest {

    private RuntimeHints hints;

    @BeforeEach
    void setUp() {
        hints = new RuntimeHints();
        new NativeHints.Registrar().registerHints(hints, getClass().getClassLoader());
    }

    @Test
    void registersTenantForReflection() {
        assertThat(RuntimeHintsPredicates.reflection().onType(Tenant.class)).accepts(hints);
    }

    @Test
    void registersUserRepresentationForReflection() {
        assertThat(RuntimeHintsPredicates.reflection().onType(UserRepresentation.class)).accepts(hints);
    }

    @Test
    void registersCredentialRepresentationForReflection() {
        assertThat(RuntimeHintsPredicates.reflection().onType(CredentialRepresentation.class)).accepts(hints);
    }

    @Test
    void registerIfPresent_skipsUnknownClass() {
        // The registrar should not throw for unknown classes.
        // Re-run with a fresh RuntimeHints to confirm no error on nonexistent classes.
        RuntimeHints freshHints = new RuntimeHints();
        assertThatCode(() -> new NativeHints.Registrar().registerHints(freshHints, getClass().getClassLoader()))
                .doesNotThrowAnyException();
    }
}
