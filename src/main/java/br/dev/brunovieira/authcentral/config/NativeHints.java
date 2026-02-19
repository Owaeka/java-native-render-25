package br.dev.brunovieira.authcentral.config;

import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;

@Configuration
@ImportRuntimeHints(NativeHints.Registrar.class)
public class NativeHints {

    static class Registrar implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            // Keycloak representations used via reflection by JAX-RS / Jackson
            hints.reflection().registerType(UserRepresentation.class,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);

            hints.reflection().registerType(CredentialRepresentation.class,
                    MemberCategory.DECLARED_FIELDS,
                    MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                    MemberCategory.INVOKE_DECLARED_METHODS);

            // Keycloak admin client internal types that may be accessed reflectively
            registerIfPresent(hints, classLoader,
                    "org.keycloak.representations.idm.RoleRepresentation",
                    "org.keycloak.representations.idm.GroupRepresentation",
                    "org.keycloak.representations.idm.ClientRepresentation",
                    "org.keycloak.representations.idm.RealmRepresentation",
                    "org.keycloak.representations.AccessToken",
                    "org.keycloak.representations.IDToken",
                    "org.keycloak.representations.RefreshToken",
                    "org.keycloak.representations.adapters.config.AdapterConfig",
                    "org.keycloak.jose.jws.JWSHeader"
            );

            // Bucket4j types used by the rate limiting infrastructure
            registerIfPresent(hints, classLoader,
                    "io.github.bucket4j.BucketConfiguration",
                    "io.github.bucket4j.Bandwidth",
                    "io.github.bucket4j.Refill",
                    "io.github.bucket4j.grid.GridBucketState"
            );
        }

        private void registerIfPresent(RuntimeHints hints, ClassLoader classLoader, String... classNames) {
            for (String className : classNames) {
                try {
                    Class<?> clazz = Class.forName(className, false, classLoader);
                    hints.reflection().registerType(clazz,
                            MemberCategory.DECLARED_FIELDS,
                            MemberCategory.INVOKE_DECLARED_CONSTRUCTORS,
                            MemberCategory.INVOKE_DECLARED_METHODS);
                } catch (ClassNotFoundException e) {
                    // Class not on classpath — skip
                }
            }
        }
    }
}
