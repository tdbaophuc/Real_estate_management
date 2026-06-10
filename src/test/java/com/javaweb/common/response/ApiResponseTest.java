package com.javaweb.common.response;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResponseTest {

    @Test
    void shouldCreateSuccessfulResponse() {
        ApiResponse<String> response = ApiResponse.success("created", "payload");

        assertThat(response.success()).isTrue();
        assertThat(response.code()).isEqualTo("SUCCESS");
        assertThat(response.message()).isEqualTo("created");
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.timestamp()).isNotNull();
    }
}
