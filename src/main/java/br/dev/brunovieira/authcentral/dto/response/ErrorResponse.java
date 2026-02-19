package br.dev.brunovieira.authcentral.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error response")
public class ErrorResponse {

    @Schema(description = "Error status", example = "error")
    @Builder.Default
    private String status = "error";

    @Schema(description = "Error message", example = "Invalid credentials")
    private String message;

    @Schema(description = "HTTP status code", example = "400")
    private Integer code;

    @Schema(description = "Detailed error information")
    private String details;

    @Schema(description = "Field-level validation errors")
    private Map<String, String> fieldErrors;

    @Schema(description = "Timestamp of the error")
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
