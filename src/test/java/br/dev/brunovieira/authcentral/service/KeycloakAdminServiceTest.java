package br.dev.brunovieira.authcentral.service;

import br.dev.brunovieira.authcentral.TestFixtures;
import br.dev.brunovieira.authcentral.exception.AuthenticationException;
import br.dev.brunovieira.authcentral.exception.UserAlreadyExistsException;
import br.dev.brunovieira.authcentral.exception.UserNotFoundException;
import br.dev.brunovieira.authcentral.model.Tenant;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import org.springframework.util.MultiValueMap;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminServiceTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private KeycloakAdminService service;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        service = new KeycloakAdminService(restClient);
        tenant = TestFixtures.tenant();
    }

    // --- createKeycloakClient / closeKeycloakClients ---
    // Cannot easily test createKeycloakClient because it calls KeycloakBuilder.builder() which is static.
    // We test closeKeycloakClients with an empty cache (no error path).

    @Test
    void closeKeycloakClients_emptyCache_doesNotThrow() {
        assertThatCode(() -> service.closeKeycloakClients()).doesNotThrowAnyException();
    }

    // --- registerUser ---

    @Test
    void registerUser_success() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserResource userResource = mock(UserResource.class);
        Response response = mock(Response.class);

        // We need to inject a Keycloak mock into the ConcurrentHashMap.
        // Use reflection to set keycloakClients directly.
        setKeycloakClient(keycloak);

        when(keycloak.realm(tenant.getRealmName())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.search("user@test.com", true)).thenReturn(Collections.emptyList());
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(201);
        when(response.getLocation()).thenReturn(URI.create("http://localhost/users/user-id-123"));
        when(usersResource.get("user-id-123")).thenReturn(userResource);

        String userId = service.registerUser(tenant, "user@test.com", "John", "Doe", "password");

        assertThat(userId).isEqualTo("user-id-123");
        verify(userResource).resetPassword(any());
        verify(response).close();
    }

    @Test
    void registerUser_userAlreadyExists() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserRepresentation existingUser = new UserRepresentation();

        setKeycloakClient(keycloak);

        when(keycloak.realm(tenant.getRealmName())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.search("user@test.com", true)).thenReturn(List.of(existingUser));

        assertThatThrownBy(() -> service.registerUser(tenant, "user@test.com", "John", "Doe", "password"))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void registerUser_failedStatus() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        Response response = mock(Response.class);
        Response.StatusType statusInfo = mock(Response.StatusType.class);

        setKeycloakClient(keycloak);

        when(keycloak.realm(tenant.getRealmName())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.search("user@test.com", true)).thenReturn(Collections.emptyList());
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(response);
        when(response.getStatus()).thenReturn(409);
        when(response.getStatusInfo()).thenReturn(statusInfo);

        assertThatThrownBy(() -> service.registerUser(tenant, "user@test.com", "John", "Doe", "password"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to create user");
    }

    // --- authenticateUser ---

    @Test
    @SuppressWarnings("unchecked")
    void authenticateUser_success() {
        setupRestClientPost();

        Map<String, Object> tokenMap = Map.of("access_token", "tok", "token_type", "Bearer");
        when(responseSpec.body(Map.class)).thenReturn(tokenMap);

        Map<String, Object> result = service.authenticateUser(tenant, "user@test.com", "password");

        assertThat(result).containsEntry("access_token", "tok");
    }

    @Test
    @SuppressWarnings("unchecked")
    void authenticateUser_nullResponse() {
        setupRestClientPost();

        when(responseSpec.body(Map.class)).thenReturn(null);

        assertThatThrownBy(() -> service.authenticateUser(tenant, "user@test.com", "password"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Failed to authenticate user");
    }

    @Test
    @SuppressWarnings("unchecked")
    void authenticateUser_authException_rethrown() {
        setupRestClientPost();

        when(responseSpec.body(Map.class)).thenThrow(new AuthenticationException("Invalid credentials"));

        assertThatThrownBy(() -> service.authenticateUser(tenant, "user@test.com", "password"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    @SuppressWarnings("unchecked")
    void authenticateUser_unexpectedException() {
        setupRestClientPost();

        when(responseSpec.body(Map.class)).thenThrow(new RuntimeException("Connection refused"));

        assertThatThrownBy(() -> service.authenticateUser(tenant, "user@test.com", "password"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Authentication failed");
    }

    // --- refreshToken ---

    @Test
    @SuppressWarnings("unchecked")
    void refreshToken_success() {
        setupRestClientPost();

        Map<String, Object> tokenMap = Map.of("access_token", "new-tok");
        when(responseSpec.body(Map.class)).thenReturn(tokenMap);

        Map<String, Object> result = service.refreshToken(tenant, "refresh-tok");

        assertThat(result).containsEntry("access_token", "new-tok");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshToken_nullResponse() {
        setupRestClientPost();

        when(responseSpec.body(Map.class)).thenReturn(null);

        assertThatThrownBy(() -> service.refreshToken(tenant, "refresh-tok"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Failed to refresh token");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshToken_authException() {
        setupRestClientPost();

        when(responseSpec.body(Map.class)).thenThrow(new AuthenticationException("Expired"));

        assertThatThrownBy(() -> service.refreshToken(tenant, "refresh-tok"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Expired");
    }

    @Test
    @SuppressWarnings("unchecked")
    void refreshToken_unexpectedException() {
        setupRestClientPost();

        when(responseSpec.body(Map.class)).thenThrow(new RuntimeException("Timeout"));

        assertThatThrownBy(() -> service.refreshToken(tenant, "refresh-tok"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessageContaining("Token refresh failed");
    }

    // --- logoutUser ---

    @Test
    void logoutUser_success() {
        setupRestClientPostForLogout();
        when(responseSpec.toBodilessEntity()).thenReturn(null);

        assertThatCode(() -> service.logoutUser(tenant, "refresh-tok")).doesNotThrowAnyException();
    }

    @Test
    void logoutUser_failure_swallowed() {
        setupRestClientPostForLogout();
        when(responseSpec.toBodilessEntity()).thenThrow(new RuntimeException("Network error"));

        assertThatCode(() -> service.logoutUser(tenant, "refresh-tok")).doesNotThrowAnyException();
    }

    // --- getUserByEmail ---

    @Test
    void getUserByEmail_found() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);
        UserRepresentation user = new UserRepresentation();
        user.setEmail("user@test.com");

        setKeycloakClient(keycloak);

        when(keycloak.realm(tenant.getRealmName())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.search("user@test.com", true)).thenReturn(List.of(user));

        UserRepresentation result = service.getUserByEmail(tenant, "user@test.com");

        assertThat(result.getEmail()).isEqualTo("user@test.com");
    }

    @Test
    void getUserByEmail_notFound() {
        Keycloak keycloak = mock(Keycloak.class);
        RealmResource realmResource = mock(RealmResource.class);
        UsersResource usersResource = mock(UsersResource.class);

        setKeycloakClient(keycloak);

        when(keycloak.realm(tenant.getRealmName())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
        when(usersResource.search("notfound@test.com", true)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> service.getUserByEmail(tenant, "notfound@test.com"))
                .isInstanceOf(UserNotFoundException.class);
    }

    // --- helpers ---

    private void setKeycloakClient(Keycloak keycloak) {
        try {
            var field = KeycloakAdminService.class.getDeclaredField("keycloakClients");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            var map = (java.util.concurrent.ConcurrentHashMap<Long, Keycloak>) field.get(service);
            map.put(tenant.getId(), keycloak);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void setupRestClientPostForLogout() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(MultiValueMap.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    @SuppressWarnings("unchecked")
    private void setupRestClientPost() {
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(MediaType.APPLICATION_FORM_URLENCODED)).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(MultiValueMap.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.onStatus(any(), any())).thenReturn(responseSpec);
    }
}
