package com.javaweb.notification.email;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmailTemplateRendererTest {

    @Test
    void shouldRenderKnownTemplateVariables() {
        EmailTemplateRenderer renderer = new EmailTemplateRenderer();

        assertThat(renderer.render(
                "Hello {{name}}, appointment {{code}} is ready.",
                Map.of("name", "Customer", "code", "APT-28")
        )).isEqualTo("Hello Customer, appointment APT-28 is ready.");
    }
}
