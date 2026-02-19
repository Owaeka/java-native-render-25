package br.dev.brunovieira.authcentral.config;

import br.dev.brunovieira.authcentral.model.Tenant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class TenantPropertiesTest {

    @Test
    void buildLookup_validEntries() {
        TenantProperties props = new TenantProperties();

        TenantProperties.TenantEntry entry = new TenantProperties.TenantEntry();
        entry.setKey("tenant-a");
        entry.setName("Tenant A");
        entry.setRealmName("realm-a");
        entry.setClientId("client-a");
        entry.setClientSecret("secret-a");
        entry.setKeycloakBaseUrl("http://localhost:8080");

        props.setTenants(List.of(entry));
        props.buildLookup();

        Optional<Tenant> result = props.findByKey("tenant-a");
        assertThat(result).isPresent();
        assertThat(result.get().getTenantName()).isEqualTo("Tenant A");
        assertThat(result.get().getRealmName()).isEqualTo("realm-a");
        assertThat(result.get().getIsActive()).isTrue();
    }

    @Test
    void buildLookup_skipsBlankKeys() {
        TenantProperties props = new TenantProperties();

        TenantProperties.TenantEntry blankEntry = new TenantProperties.TenantEntry();
        blankEntry.setKey("   ");
        blankEntry.setName("Blank");

        TenantProperties.TenantEntry validEntry = new TenantProperties.TenantEntry();
        validEntry.setKey("valid");
        validEntry.setName("Valid");
        validEntry.setRealmName("realm");
        validEntry.setClientId("cid");
        validEntry.setClientSecret("secret");
        validEntry.setKeycloakBaseUrl("http://localhost");

        props.setTenants(List.of(blankEntry, validEntry));
        props.buildLookup();

        assertThat(props.findByKey("   ")).isEmpty();
        assertThat(props.findByKey("valid")).isPresent();
    }

    @Test
    void buildLookup_skipsNullKeys() {
        TenantProperties props = new TenantProperties();

        TenantProperties.TenantEntry nullEntry = new TenantProperties.TenantEntry();
        nullEntry.setKey(null);
        nullEntry.setName("Null Key");

        props.setTenants(List.of(nullEntry));
        props.buildLookup();

        assertThat(props.findByKey(null)).isEmpty();
    }

    @Test
    void findByKey_notFound() {
        TenantProperties props = new TenantProperties();
        props.setTenants(List.of());
        props.buildLookup();

        assertThat(props.findByKey("nonexistent")).isEmpty();
    }
}
