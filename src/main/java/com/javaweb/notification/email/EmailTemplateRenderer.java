package com.javaweb.notification.email;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class EmailTemplateRenderer {

    public String render(String template, Map<String, String> variables) {
        String rendered = template;
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            rendered = rendered.replace(
                    "{{" + variable.getKey() + "}}",
                    variable.getValue() == null ? "" : variable.getValue()
            );
        }
        return rendered;
    }
}
