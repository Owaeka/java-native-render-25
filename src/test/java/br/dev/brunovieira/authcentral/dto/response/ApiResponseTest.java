package br.dev.brunovieira.authcentral.dto.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class ApiResponseTest {

    @Test
    void success_withMessageAndData() {
        ApiResponse<String> response = ApiResponse.success("Done", "payload");

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Done");
        assertThat(response.getData()).isEqualTo("payload");
        assertThat(response.getTimestamp()).isNotNull();
    }

    @Test
    void success_withDataOnly() {
        ApiResponse<Integer> response = ApiResponse.success(42);

        assertThat(response.getStatus()).isEqualTo("success");
        assertThat(response.getMessage()).isEqualTo("Operation completed successfully");
        assertThat(response.getData()).isEqualTo(42);
    }

    @Test
    void error_message() {
        ApiResponse<Void> response = ApiResponse.error("Something went wrong");

        assertThat(response.getStatus()).isEqualTo("error");
        assertThat(response.getMessage()).isEqualTo("Something went wrong");
        assertThat(response.getData()).isNull();
        assertThat(response.getTimestamp()).isNotNull();
    }
}
