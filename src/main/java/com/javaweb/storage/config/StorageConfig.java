package com.javaweb.storage.config;

import com.javaweb.storage.enums.StorageProvider;
import com.javaweb.storage.service.FileStorageService;
import com.javaweb.storage.service.LocalFileStorageService;
import com.javaweb.storage.service.R2FileStorageService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

import java.util.Locale;

@Configuration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageConfig implements WebMvcConfigurer {
    private final StorageProperties properties;

    public StorageConfig(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        if (properties.provider() != StorageProvider.LOCAL) {
            return;
        }
        String publicDirectory = properties.localRoot()
                .toAbsolutePath()
                .normalize()
                .resolve("public")
                .toUri()
                .toString();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(publicDirectory);
    }

    @Bean
    public FileStorageService fileStorageService(StorageProperties properties) {
        if (properties.provider() == StorageProvider.R2) {
            return new R2FileStorageService(properties, s3Client(properties));
        }
        return new LocalFileStorageService(properties);
    }

    private S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(properties.endpoint())
                .region(Region.of(properties.region().toLowerCase(Locale.ROOT)))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();
    }
}
