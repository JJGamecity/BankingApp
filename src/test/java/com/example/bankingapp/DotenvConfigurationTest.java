package com.example.bankingapp;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.context.annotation.Configuration;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DotenvConfigurationTest {
    @TempDir Path directory;

    @Configuration(proxyBeanMethods = false)
    static class EmptyApplication { }

    private SpringApplication application() {
        // Load Spring's real config/EnvironmentPostProcessors without starting a database or server.
        var application = new SpringApplication(EmptyApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setRegisterShutdownHook(false);
        return application;
    }

    @Test void bootAutomaticallyLoadsDotenvAndResolvesCredentialPlaceholders() throws Exception {
        Files.writeString(directory.resolve(".env"), """
                BANK_DB_PASSWORD="fixture!$#password"
                BANK_SECURITY_ADMIN_USERNAME=fixture.admin
                BANK_SECURITY_ADMIN_PASSWORD="fixture!admin"
                BANK_SECURITY_REP_USERNAME=fixture.rep
                BANK_SECURITY_REP_PASSWORD="fixture!rep"
                BANK_SECURITY_GOOGLE_ENABLED=true
                BANK_SECURITY_GOOGLE_CLIENT_ID=fixture-client
                BANK_SECURITY_GOOGLE_CLIENT_SECRET="fixture!google"
                """);
        try (var context = application().run("--springdotenv.enabled=true",
                "--springdotenv.directory=" + directory, "--spring.profiles.active=dotenv-test")) {
            var environment = context.getEnvironment();
            // Boolean assertions prevent accidental credential disclosure in test reports.
            assertTrue("fixture!$#password".equals(environment.getProperty("spring.datasource.password")), "Database dotenv mapping failed");
            assertTrue("fixture.admin".equals(environment.getProperty("bank.security.admin.username")), "Admin username mapping failed");
            assertTrue("fixture!admin".equals(environment.getProperty("bank.security.admin.password")), "Admin password mapping failed");
            assertTrue("fixture.rep".equals(environment.getProperty("spring.security.user.name")), "Representative username mapping failed");
            assertTrue("fixture!rep".equals(environment.getProperty("spring.security.user.password")), "Representative password mapping failed");
            assertTrue("true".equals(environment.getProperty("bank.security.google.enabled")), "Google enabled mapping failed");
            assertTrue("fixture-client".equals(environment.getProperty("bank.security.google.client-id")), "Google client mapping failed");
            assertTrue("fixture!google".equals(environment.getProperty("bank.security.google.client-secret")), "Google secret mapping failed");
        }
    }

    @Test void explicitConfigurationOverridesDotenv() throws Exception {
        Files.writeString(directory.resolve(".env"), "BANK_DB_PASSWORD=fixture-file-value\n");
        try (var context = application().run("--springdotenv.enabled=true",
                "--springdotenv.directory=" + directory, "--spring.profiles.active=dotenv-test",
                "--BANK_DB_PASSWORD=fixture-override")) {
            assertTrue("fixture-override".equals(context.getEnvironment().getProperty("spring.datasource.password")),
                    "Explicit configuration must override dotenv");
        }
    }

    @Test void missingDotenvIsOptionalAndDoesNotSearchTheProjectRoot() {
        try (var context = application().run("--springdotenv.enabled=true",
                "--springdotenv.directory=" + directory, "--spring.profiles.active=dotenv-test")) {
            assertTrue(context.isActive());
            assertTrue("".equals(context.getEnvironment().getProperty("bank.security.google.client-secret")),
                    "An absent test dotenv file must not load private project credentials");
        }
    }
}
