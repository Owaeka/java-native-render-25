package br.dev.brunovieira.authcentral.service;

import br.dev.brunovieira.authcentral.TestFixtures;
import br.dev.brunovieira.authcentral.dto.request.LoginRequest;
import br.dev.brunovieira.authcentral.dto.request.RegisterRequest;
import br.dev.brunovieira.authcentral.dto.response.LoginResponse;
import br.dev.brunovieira.authcentral.exception.AuthenticationException;
import br.dev.brunovieira.authcentral.model.Tenant;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private KeycloakAdminService keycloakAdminService;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private AuthService authService;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = TestFixtures.tenant();
    }

    // --- register ---

    @Test
    void register_success() {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@test.com")
                .firstName("John")
                .lastName("Doe")
                .password("password123")
                .build();

        when(keycloakAdminService.registerUser(tenant, "user@test.com", "John", "Doe", "password123"))
                .thenReturn("user-id-1");

        authService.register(tenant, request, "127.0.0.1", "TestAgent");

        verify(keycloakAdminService).registerUser(tenant, "user@test.com", "John", "Doe", "password123");
        verify(auditLogService).logSuccess(tenant, "user@test.com", "USER_REGISTER", "127.0.0.1", "TestAgent");
    }

    @Test
    void register_failure_logsAndRethrows() {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@test.com")
                .firstName("John")
                .lastName("Doe")
                .password("password123")
                .build();

        when(keycloakAdminService.registerUser(tenant, "user@test.com", "John", "Doe", "password123"))
                .thenThrow(new RuntimeException("Registration failed"));

        assertThatThrownBy(() -> authService.register(tenant, request, "127.0.0.1", "TestAgent"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Registration failed");

        verify(auditLogService).logFailure(tenant, "user@test.com", "USER_REGISTER", "127.0.0.1", "TestAgent", "Registration failed");
    }

    // --- login ---

    @Test
    @SuppressWarnings("unchecked")
    void login_success_decodesJwtAndReturns() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@test.com")
                .password("password123")
                .build();

        String payload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"email\":\"user@test.com\",\"given_name\":\"John\",\"family_name\":\"Doe\"}".getBytes());
        String jwt = "header." + payload + ".signature";

        Map<String, Object> tokenResponse = Map.of(
                "access_token", jwt,
                "refresh_token", "refresh-tok",
                "token_type", "Bearer",
                "expires_in", 1800,
                "refresh_expires_in", 3600
        );

        when(keycloakAdminService.authenticateUser(tenant, "user@test.com", "password123"))
                .thenReturn(tokenResponse);

        Map<String, Object> claims = Map.of(
                "email", "user@test.com",
                "given_name", "John",
                "family_name", "Doe"
        );
        when(jsonMapper.readValue(any(byte[].class), any(Class.class))).thenReturn(claims);

        LoginResponse response = authService.login(tenant, request, "127.0.0.1", "TestAgent");

        assertThat(response.getAccessToken()).isEqualTo(jwt);
        assertThat(response.getRefreshToken()).isEqualTo("refresh-tok");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(1800L);
        assertThat(response.getRefreshExpiresIn()).isEqualTo(3600L);
        assertThat(response.getEmail()).isEqualTo("user@test.com");
        assertThat(response.getFirstName()).isEqualTo("John");
        assertThat(response.getLastName()).isEqualTo("Doe");

        verify(auditLogService).logSuccess(tenant, "user@test.com", "USER_LOGIN", "127.0.0.1", "TestAgent");
    }

    @Test
    void login_failure_logsAndRethrows() {
        LoginRequest request = LoginRequest.builder()
                .email("user@test.com")
                .password("wrong")
                .build();

        when(keycloakAdminService.authenticateUser(tenant, "user@test.com", "wrong"))
                .thenThrow(new AuthenticationException("Invalid credentials"));

        assertThatThrownBy(() -> authService.login(tenant, request, "127.0.0.1", "TestAgent"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid credentials");

        verify(auditLogService).logFailure(tenant, "user@test.com", "USER_LOGIN", "127.0.0.1", "TestAgent", "Invalid credentials");
    }

    @Test
    void login_invalidJwtFormat_throwsAuthenticationException() {
        LoginRequest request = LoginRequest.builder()
                .email("user@test.com")
                .password("password123")
                .build();

        // JWT with only 2 parts — triggers parts.length != 3 → AuthenticationException
        Map<String, Object> tokenResponse = Map.of(
                "access_token", "invalid.jwt",
                "refresh_token", "refresh-tok",
                "token_type", "Bearer",
                "expires_in", 1800,
                "refresh_expires_in", 3600
        );

        when(keycloakAdminService.authenticateUser(tenant, "user@test.com", "password123"))
                .thenReturn(tokenResponse);

        assertThatThrownBy(() -> authService.login(tenant, request, "127.0.0.1", "TestAgent"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Invalid JWT format");

        verify(auditLogService).logFailure(eq(tenant), eq("user@test.com"), eq("USER_LOGIN"),
                eq("127.0.0.1"), eq("TestAgent"), eq("Invalid JWT format"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void login_jwtDecodeException_fallsBackToEmptyClaims() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("user@test.com")
                .password("password123")
                .build();

        // Valid 3-part JWT but base64 decode of payload throws exception in jsonMapper
        Map<String, Object> tokenResponse = Map.of(
                "access_token", "header.payload.signature",
                "refresh_token", "refresh-tok",
                "token_type", "Bearer",
                "expires_in", 1800,
                "refresh_expires_in", 3600
        );

        when(keycloakAdminService.authenticateUser(tenant, "user@test.com", "password123"))
                .thenReturn(tokenResponse);

        when(jsonMapper.readValue(any(byte[].class), any(Class.class)))
                .thenThrow(new RuntimeException("Bad JSON"));

        // Falls back to empty claims — email comes from request.getEmail() via getOrDefault
        LoginResponse response = authService.login(tenant, request, "127.0.0.1", "TestAgent");

        assertThat(response.getEmail()).isEqualTo("user@test.com");
        assertThat(response.getFirstName()).isNull();
        assertThat(response.getLastName()).isNull();

        verify(auditLogService).logSuccess(tenant, "user@test.com", "USER_LOGIN", "127.0.0.1", "TestAgent");
    }

    // --- refreshToken ---

    @Test
    void refreshToken_success() {
        Map<String, Object> tokenResponse = Map.of(
                "access_token", "new-access",
                "refresh_token", "new-refresh",
                "token_type", "Bearer",
                "expires_in", 1800,
                "refresh_expires_in", 3600
        );

        when(keycloakAdminService.refreshToken(tenant, "old-refresh")).thenReturn(tokenResponse);

        LoginResponse response = authService.refreshToken(tenant, "old-refresh", "127.0.0.1", "TestAgent");

        assertThat(response.getAccessToken()).isEqualTo("new-access");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh");
        verify(auditLogService).logSuccess(tenant, null, "TOKEN_REFRESH", "127.0.0.1", "TestAgent");
    }

    @Test
    void refreshToken_failure_logsAndRethrows() {
        when(keycloakAdminService.refreshToken(tenant, "bad-token"))
                .thenThrow(new AuthenticationException("Token expired"));

        assertThatThrownBy(() -> authService.refreshToken(tenant, "bad-token", "127.0.0.1", "TestAgent"))
                .isInstanceOf(AuthenticationException.class)
                .hasMessage("Token expired");

        verify(auditLogService).logFailure(tenant, null, "TOKEN_REFRESH", "127.0.0.1", "TestAgent", "Token expired");
    }

    // --- logout ---

    @Test
    void logout_success() {
        authService.logout(tenant, "refresh-tok", "127.0.0.1", "TestAgent");

        verify(keycloakAdminService).logoutUser(tenant, "refresh-tok");
        verify(auditLogService).logSuccess(tenant, null, "USER_LOGOUT", "127.0.0.1", "TestAgent");
    }

    @Test
    void logout_failure_doesNotThrow() {
        doThrow(new RuntimeException("Logout error"))
                .when(keycloakAdminService).logoutUser(tenant, "refresh-tok");

        // Should not throw
        assertThatCode(() -> authService.logout(tenant, "refresh-tok", "127.0.0.1", "TestAgent"))
                .doesNotThrowAnyException();

        verify(auditLogService).logFailure(tenant, null, "USER_LOGOUT", "127.0.0.1", "TestAgent", "Logout error");
    }
}
