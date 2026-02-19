package br.dev.brunovieira.authcentral.controller;

import br.dev.brunovieira.authcentral.TestFixtures;
import br.dev.brunovieira.authcentral.dto.request.LoginRequest;
import br.dev.brunovieira.authcentral.dto.request.RefreshTokenRequest;
import br.dev.brunovieira.authcentral.dto.request.RegisterRequest;
import br.dev.brunovieira.authcentral.dto.response.ApiResponse;
import br.dev.brunovieira.authcentral.dto.response.LoginResponse;
import br.dev.brunovieira.authcentral.model.Tenant;
import br.dev.brunovieira.authcentral.service.AuthService;
import br.dev.brunovieira.authcentral.util.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private HttpServletRequest httpRequest;

    private AuthController controller;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService);
        tenant = TestFixtures.tenant();
        TenantContext.setCurrentTenant(tenant);

        when(httpRequest.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1");
        when(httpRequest.getHeader("User-Agent")).thenReturn("TestAgent");
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void register_returnsCreated() {
        RegisterRequest request = RegisterRequest.builder()
                .email("user@test.com")
                .firstName("John")
                .lastName("Doe")
                .password("password123")
                .build();

        ResponseEntity<ApiResponse<Void>> response = controller.register(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("success");
        verify(authService).register(eq(tenant), eq(request), eq("10.0.0.1"), eq("TestAgent"));
    }

    @Test
    void login_returnsOk() {
        LoginRequest request = LoginRequest.builder()
                .email("user@test.com")
                .password("password123")
                .build();

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken("access-tok")
                .refreshToken("refresh-tok")
                .tokenType("Bearer")
                .expiresIn(1800L)
                .build();

        when(authService.login(eq(tenant), eq(request), eq("10.0.0.1"), eq("TestAgent")))
                .thenReturn(loginResponse);

        ResponseEntity<ApiResponse<LoginResponse>> response = controller.login(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("access-tok");
    }

    @Test
    void refreshToken_returnsOk() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("old-refresh")
                .build();

        LoginResponse loginResponse = LoginResponse.builder()
                .accessToken("new-access")
                .refreshToken("new-refresh")
                .tokenType("Bearer")
                .expiresIn(1800L)
                .build();

        when(authService.refreshToken(eq(tenant), eq("old-refresh"), eq("10.0.0.1"), eq("TestAgent")))
                .thenReturn(loginResponse);

        ResponseEntity<ApiResponse<LoginResponse>> response = controller.refreshToken(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData().getAccessToken()).isEqualTo("new-access");
    }

    @Test
    void logout_returnsOk() {
        RefreshTokenRequest request = RefreshTokenRequest.builder()
                .refreshToken("refresh-tok")
                .build();

        ResponseEntity<ApiResponse<Void>> response = controller.logout(request, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("success");
        verify(authService).logout(eq(tenant), eq("refresh-tok"), eq("10.0.0.1"), eq("TestAgent"));
    }
}
