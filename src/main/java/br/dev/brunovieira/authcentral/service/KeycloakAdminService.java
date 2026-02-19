package br.dev.brunovieira.authcentral.service;

import br.dev.brunovieira.authcentral.exception.AuthenticationException;
import br.dev.brunovieira.authcentral.exception.UserAlreadyExistsException;
import br.dev.brunovieira.authcentral.exception.UserNotFoundException;
import br.dev.brunovieira.authcentral.model.Tenant;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import jakarta.ws.rs.core.Response;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class KeycloakAdminService {

    private final RestClient restClient;
    private final ConcurrentHashMap<Long, Keycloak> keycloakClients = new ConcurrentHashMap<>();

    public KeycloakAdminService(RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Get or create a Keycloak admin client for the tenant.
     * Cached in-memory via ConcurrentHashMap (Keycloak objects contain HTTP clients
     * and thread pools that are not serializable to Redis).
     */
    public Keycloak createKeycloakClient(Tenant tenant) {
        return keycloakClients.computeIfAbsent(tenant.getId(), id -> {
            log.info("Creating Keycloak client for tenant: {}", tenant.getTenantName());
            return KeycloakBuilder.builder()
                    .serverUrl(tenant.getKeycloakBaseUrl())
                    .realm(tenant.getRealmName())
                    .clientId(tenant.getClientId())
                    .clientSecret(tenant.getClientSecret())
                    .grantType("client_credentials")
                    .build();
        });
    }

    @PreDestroy
    public void closeKeycloakClients() {
        log.info("Closing {} cached Keycloak clients", keycloakClients.size());
        keycloakClients.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Error closing Keycloak client: {}", e.getMessage());
            }
        });
        keycloakClients.clear();
    }

    /**
     * Register a new user in Keycloak
     */
    public String registerUser(Tenant tenant, String email, String firstName, String lastName, String password) {
        log.info("Registering user {} in realm {}", email, tenant.getRealmName());

        Keycloak keycloak = createKeycloakClient(tenant);
        RealmResource realmResource = keycloak.realm(tenant.getRealmName());
        UsersResource usersResource = realmResource.users();

        // Check if user already exists
        List<UserRepresentation> existingUsers = usersResource.search(email, true);
        if (!existingUsers.isEmpty()) {
            log.warn("User {} already exists in realm {}", email, tenant.getRealmName());
            throw new UserAlreadyExistsException("User with email " + email + " already exists");
        }

        // Create user representation
        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername(email);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmailVerified(true);

        // Create user
        Response response = usersResource.create(user);

        if (response.getStatus() != 201) {
            log.error("Failed to create user: {}", response.getStatusInfo());
            throw new RuntimeException("Failed to create user: " + response.getStatusInfo());
        }

        String userId = extractUserIdFromLocation(response.getLocation().getPath());
        log.info("User created with ID: {}", userId);

        // Set password
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);

        UserResource userResource = usersResource.get(userId);
        userResource.resetPassword(credential);

        log.info("Password set for user: {}", userId);
        response.close();

        return userId;
    }

    /**
     * Authenticate user and get tokens
     */
    public Map<String, Object> authenticateUser(Tenant tenant, String email, String password) {
        log.info("Authenticating user {} in realm {}", email, tenant.getRealmName());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", tenant.getClientId());
        body.add("client_secret", tenant.getClientSecret());
        body.add("grant_type", "password");
        body.add("username", email);
        body.add("password", password);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restClient.post()
                    .uri(tokenUrl(tenant))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        if (res.getStatusCode().value() == 401) {
                            throw new AuthenticationException("Invalid credentials");
                        }
                        throw new AuthenticationException("Authentication failed: " + res.getStatusCode());
                    })
                    .body(Map.class);

            if (tokenResponse != null) {
                log.info("User {} authenticated successfully", email);
                return tokenResponse;
            }

            throw new AuthenticationException("Failed to authenticate user");

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error during authentication: {}", e.getMessage());
            throw new AuthenticationException("Authentication failed: " + e.getMessage(), e);
        }
    }

    /**
     * Refresh access token using refresh token
     */
    public Map<String, Object> refreshToken(Tenant tenant, String refreshToken) {
        log.info("Refreshing token for tenant {}", tenant.getTenantName());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", tenant.getClientId());
        body.add("client_secret", tenant.getClientSecret());
        body.add("grant_type", "refresh_token");
        body.add("refresh_token", refreshToken);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> tokenResponse = restClient.post()
                    .uri(tokenUrl(tenant))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new AuthenticationException("Invalid or expired refresh token");
                    })
                    .body(Map.class);

            if (tokenResponse != null) {
                log.info("Token refreshed successfully");
                return tokenResponse;
            }

            throw new AuthenticationException("Failed to refresh token");

        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new AuthenticationException("Token refresh failed: " + e.getMessage());
        }
    }

    /**
     * Logout user (revoke tokens)
     */
    public void logoutUser(Tenant tenant, String refreshToken) {
        log.info("Logging out user for tenant {}", tenant.getTenantName());

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("client_id", tenant.getClientId());
        body.add("client_secret", tenant.getClientSecret());
        body.add("refresh_token", refreshToken);

        try {
            restClient.post()
                    .uri(logoutUrl(tenant))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
            log.info("User logged out successfully");
        } catch (Exception e) {
            log.error("Logout failed: {}", e.getMessage());
            // Don't throw exception for logout failures
        }
    }

    /**
     * Get user information by email
     */
    public UserRepresentation getUserByEmail(Tenant tenant, String email) {
        Keycloak keycloak = createKeycloakClient(tenant);
        RealmResource realmResource = keycloak.realm(tenant.getRealmName());
        UsersResource usersResource = realmResource.users();

        List<UserRepresentation> users = usersResource.search(email, true);
        if (users.isEmpty()) {
            throw new UserNotFoundException("User with email " + email + " not found");
        }

        return users.get(0);
    }

    // --- URL helpers ---

    private String tokenUrl(Tenant tenant) {
        return tenant.getKeycloakBaseUrl() + "/realms/" + tenant.getRealmName() + "/protocol/openid-connect/token";
    }

    private String logoutUrl(Tenant tenant) {
        return tenant.getKeycloakBaseUrl() + "/realms/" + tenant.getRealmName() + "/protocol/openid-connect/logout";
    }

    /**
     * Extract user ID from Location header
     */
    private String extractUserIdFromLocation(String locationPath) {
        String[] parts = locationPath.split("/");
        return parts[parts.length - 1];
    }
}
