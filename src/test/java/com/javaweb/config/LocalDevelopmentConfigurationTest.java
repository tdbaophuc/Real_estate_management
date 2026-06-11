package com.javaweb.config;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDevelopmentConfigurationTest {
    private static final Path PROJECT_ROOT = Path.of("").toAbsolutePath();

    @Test
    void composeShouldProvideRequiredLocalServices() throws IOException {
        Map<String, Object> compose = readYaml(PROJECT_ROOT.resolve("compose.yaml"));
        Map<String, Object> services = mapValue(compose, "services");

        assertThat(services).containsKeys(
                "postgres",
                "redis",
                "minio",
                "minio-init",
                "mailhog"
        );
        assertThat(mapValue(services, "postgres"))
                .containsEntry("image", "postgres:16-alpine")
                .containsKey("healthcheck");
        assertThat(mapValue(services, "redis"))
                .containsEntry("image", "redis:7.4-alpine")
                .containsKey("healthcheck");
        assertThat(mapValue(services, "minio")).containsKey("healthcheck");
        assertThat(mapValue(compose, "volumes"))
                .containsKeys("postgres-data", "redis-data", "minio-data");
    }

    @Test
    void localCredentialsShouldComeFromIgnoredEnvironmentFile()
            throws IOException {
        String compose = Files.readString(PROJECT_ROOT.resolve("compose.yaml"));
        String devConfig = Files.readString(PROJECT_ROOT.resolve(
                "src/main/resources/application-dev.yml"
        ));
        String gitignore = Files.readString(PROJECT_ROOT.resolve(".gitignore"));
        Set<String> environmentKeys = Files.readAllLines(
                        PROJECT_ROOT.resolve(".env.example")
                ).stream()
                .filter(line -> !line.isBlank() && !line.startsWith("#"))
                .map(line -> line.substring(0, line.indexOf('=')))
                .collect(java.util.stream.Collectors.toSet());

        assertThat(environmentKeys).contains(
                "DB_NAME",
                "DB_USERNAME",
                "DB_PASSWORD",
                "DB_URL",
                "MINIO_ROOT_USER",
                "MINIO_ROOT_PASSWORD",
                "MINIO_BUCKET"
        );
        assertThat(compose)
                .contains("${DB_PASSWORD:?")
                .contains("${MINIO_ROOT_PASSWORD:?");
        assertThat(devConfig)
                .contains("password: ${DB_PASSWORD}")
                .doesNotContain("password: ${DB_PASSWORD:123456}");
        assertThat(gitignore.lines()).contains(".env");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readYaml(Path path) throws IOException {
        try (InputStream input = Files.newInputStream(path)) {
            return new Yaml().load(input);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(
            Map<String, Object> source,
            String key
    ) {
        return (Map<String, Object>) source.get(key);
    }
}
