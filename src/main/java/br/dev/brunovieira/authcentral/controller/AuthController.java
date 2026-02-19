package br.dev.brunovieira.authcentral.controller;

import br.dev.brunovieira.authcentral.dto.request.LoginRequest;
import br.dev.brunovieira.authcentral.dto.request.RefreshTokenRequest;
import br.dev.brunovieira.authcentral.dto.request.RegisterRequest;
import br.dev.brunovieira.authcentral.dto.response.ApiResponse;
import br.dev.brunovieira.authcentral.dto.response.LoginResponse;
import br.dev.brunovieira.authcentral.model.Tenant;
import br.dev.brunovieira.authcentral.service.AuthService;
import br.dev.brunovieira.authcentral.util.RequestUtils;
import br.dev.brunovieira.authcentral.util.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Authentication", description = "Authentication endpoints for user registration, login, and token management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(
            summary = "Register a new user",
            description = "Creates a new user account in the tenant's Keycloak realm"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = ApiResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "Invalid request or user already exists"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid or missing tenant key"
            )
    })
    public ResponseEntity<ApiResponse<Void>> register(
            @Parameter(description = "Registration details", required = true)
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        Tenant tenant = TenantContext.getCurrentTenant();
        String ipAddress = RequestUtils.getClientIpAddress(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);

        authService.register(tenant, request, ipAddress, userAgent);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", null));
    }

    @PostMapping("/login")
    @Operation(
            summary = "User login",
            description = "Authenticates a user and returns JWT access token and refresh token"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Login successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid credentials"
            )
    })
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Parameter(description = "Login credentials", required = true)
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        Tenant tenant = TenantContext.getCurrentTenant();
        String ipAddress = RequestUtils.getClientIpAddress(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);

        LoginResponse response = authService.login(tenant, request, ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success("Login successful", response));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Refresh access token",
            description = "Uses a refresh token to obtain a new access token"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "Invalid or expired refresh token"
            )
    })
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(
            @Parameter(description = "Refresh token", required = true)
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        Tenant tenant = TenantContext.getCurrentTenant();
        String ipAddress = RequestUtils.getClientIpAddress(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);

        LoginResponse response = authService.refreshToken(
                tenant,
                request.getRefreshToken(),
                ipAddress,
                userAgent
        );

        return ResponseEntity.ok(ApiResponse.success("Token refreshed successfully", response));
    }

    @PostMapping("/logout")
    @Operation(
            summary = "User logout",
            description = "Logs out a user by revoking their refresh token"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Logout successful"
            )
    })
    public ResponseEntity<ApiResponse<Void>> logout(
            @Parameter(description = "Refresh token", required = true)
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpRequest
    ) {
        Tenant tenant = TenantContext.getCurrentTenant();
        String ipAddress = RequestUtils.getClientIpAddress(httpRequest);
        String userAgent = RequestUtils.getUserAgent(httpRequest);

        authService.logout(tenant, request.getRefreshToken(), ipAddress, userAgent);

        return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
    }
}
