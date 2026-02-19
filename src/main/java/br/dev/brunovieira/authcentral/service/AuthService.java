package br.dev.brunovieira.authcentral.service;

import br.dev.brunovieira.authcentral.dto.request.LoginRequest;
import br.dev.brunovieira.authcentral.dto.request.RegisterRequest;
import br.dev.brunovieira.authcentral.dto.response.LoginResponse;
import br.dev.brunovieira.authcentral.exception.AuthenticationException;
import br.dev.brunovieira.authcentral.model.AuditAction;
import br.dev.brunovieira.authcentral.model.Tenant;
import tools.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final KeycloakAdminService keycloakAdminService;
    private final AuditLogService auditLogService;
    private final JsonMapper jsonMapper;

    /**
     * Register a new user
     */
    public void register(Tenant tenant, RegisterRequest request, String ipAddress, String userAgent) {
        try {
            String userId = keycloakAdminService.registerUser(
                    tenant,
                    request.getEmail(),
                    request.getFirstName(),
                    request.getLastName(),
                    request.getPassword()
            );

            auditLogService.logSuccess(
                    tenant,
                    request.getEmail(),
                    AuditAction.USER_REGISTER.name(),
                    ipAddress,
                    userAgent
            );

            log.info("User registered successfully: {}", request.getEmail());

        } catch (Exception e) {
            auditLogService.logFailure(
                    tenant,
                    request.getEmail(),
                    AuditAction.USER_REGISTER.name(),
                    ipAddress,
                    userAgent,
                    e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Authenticate user and return tokens.
     * Extracts user info from the JWT access token instead of making a second Keycloak admin API call.
     */
    public LoginResponse login(Tenant tenant, LoginRequest request, String ipAddress, String userAgent) {
        try {
            Map<String, Object> tokenResponse = keycloakAdminService.authenticateUser(
                    tenant,
                    request.getEmail(),
                    request.getPassword()
            );

            // Extract user info from the JWT access token (avoids a second Keycloak round-trip)
            String accessToken = (String) tokenResponse.get("access_token");
            Map<String, Object> claims = decodeJwtPayload(accessToken);

            String email = (String) claims.getOrDefault("email", request.getEmail());
            String firstName = (String) claims.get("given_name");
            String lastName = (String) claims.get("family_name");

            auditLogService.logSuccess(
                    tenant,
                    request.getEmail(),
                    AuditAction.USER_LOGIN.name(),
                    ipAddress,
                    userAgent
            );

            log.info("User logged in successfully: {}", request.getEmail());

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken((String) tokenResponse.get("refresh_token"))
                    .tokenType((String) tokenResponse.get("token_type"))
                    .expiresIn(((Number) tokenResponse.get("expires_in")).longValue())
                    .refreshExpiresIn(((Number) tokenResponse.get("refresh_expires_in")).longValue())
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .build();

        } catch (Exception e) {
            auditLogService.logFailure(
                    tenant,
                    request.getEmail(),
                    AuditAction.USER_LOGIN.name(),
                    ipAddress,
                    userAgent,
                    e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Refresh access token
     */
    public LoginResponse refreshToken(Tenant tenant, String refreshToken, String ipAddress, String userAgent) {
        try {
            Map<String, Object> tokenResponse = keycloakAdminService.refreshToken(tenant, refreshToken);

            auditLogService.logSuccess(
                    tenant,
                    null,
                    AuditAction.TOKEN_REFRESH.name(),
                    ipAddress,
                    userAgent
            );

            log.info("Token refreshed successfully for tenant: {}", tenant.getTenantName());

            return LoginResponse.builder()
                    .accessToken((String) tokenResponse.get("access_token"))
                    .refreshToken((String) tokenResponse.get("refresh_token"))
                    .tokenType((String) tokenResponse.get("token_type"))
                    .expiresIn(((Number) tokenResponse.get("expires_in")).longValue())
                    .refreshExpiresIn(((Number) tokenResponse.get("refresh_expires_in")).longValue())
                    .build();

        } catch (Exception e) {
            auditLogService.logFailure(
                    tenant,
                    null,
                    AuditAction.TOKEN_REFRESH.name(),
                    ipAddress,
                    userAgent,
                    e.getMessage()
            );
            throw e;
        }
    }

    /**
     * Logout user
     */
    public void logout(Tenant tenant, String refreshToken, String ipAddress, String userAgent) {
        try {
            keycloakAdminService.logoutUser(tenant, refreshToken);

            auditLogService.logSuccess(
                    tenant,
                    null,
                    AuditAction.USER_LOGOUT.name(),
                    ipAddress,
                    userAgent
            );

            log.info("User logged out successfully");

        } catch (Exception e) {
            auditLogService.logFailure(
                    tenant,
                    null,
                    AuditAction.USER_LOGOUT.name(),
                    ipAddress,
                    userAgent,
                    e.getMessage()
            );
            // Don't throw for logout failures
        }
    }

    /**
     * Decode the payload segment of a JWT without signature verification.
     * Safe because we just received this token directly from Keycloak.
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeJwtPayload(String jwt) {
        try {
            String[] parts = jwt.split("\\.");
            if (parts.length != 3) {
                throw new AuthenticationException("Invalid JWT format");
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[1]);
            return jsonMapper.readValue(payload, Map.class);
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Failed to decode JWT payload, falling back to empty claims: {}", e.getMessage());
            return Map.of();
        }
    }
}
