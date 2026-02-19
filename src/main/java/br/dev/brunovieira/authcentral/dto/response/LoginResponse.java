package br.dev.brunovieira.authcentral.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login response with tokens")
public class LoginResponse {

    @JsonProperty("access_token")
    @Schema(description = "JWT access token", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @JsonProperty("refresh_token")
    @Schema(description = "Refresh token for obtaining new access tokens", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @JsonProperty("token_type")
    @Schema(description = "Token type", example = "Bearer")
    private String tokenType;

    @JsonProperty("expires_in")
    @Schema(description = "Access token expiration time in seconds", example = "1800")
    private Long expiresIn;

    @JsonProperty("refresh_expires_in")
    @Schema(description = "Refresh token expiration time in seconds", example = "1728000")
    private Long refreshExpiresIn;

    @Schema(description = "User email", example = "user@example.com")
    private String email;

    @JsonProperty("first_name")
    @Schema(description = "User first name", example = "John")
    private String firstName;

    @JsonProperty("last_name")
    @Schema(description = "User last name", example = "Doe")
    private String lastName;
}
