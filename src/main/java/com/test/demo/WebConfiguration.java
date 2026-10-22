package com.test.demo;

import java.nio.file.Path;
import java.nio.file.Files;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

    private final List<String> allowedOrigins;

    public WebConfiguration(
            @Value("${app.cors.allowed-origins:http://localhost:8081}") String allowedOrigins) {
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @Override
    public void addCorsMappings(org.springframework.web.servlet.config.annotation.CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.toArray(String[]::new))
                .allowedMethods("GET", "POST", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path webRoot = findWebRoot();
        registry.addResourceHandler(
                        "/index.html",
                        "/login.html",
                        "/home.html",
                        "/dashboard.html",
                        "/profile.html",
                        "/login-history.html",
                        "/todo.html")
                .addResourceLocations(webRoot.toUri().toString());
        registry.addResourceHandler("/css/**")
                .addResourceLocations(webRoot.resolve("css").toUri().toString());
        registry.addResourceHandler("/js/**")
                .addResourceLocations(webRoot.resolve("js").toUri().toString());
        registry.addResourceHandler("/images/**")
                .addResourceLocations(webRoot.resolve("images").toUri().toString());
    }

    Path findWebRoot() {
        Path workingDirectory = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path root = findWebRootFrom(workingDirectory);
        if (root != null) {
            return root;
        }

        try {
            Path classesDirectory = Path.of(
                    WebConfiguration.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath()
                    .normalize();
            root = findWebRootFrom(classesDirectory);
        } catch (URISyntaxException exception) {
            throw new IllegalStateException("Cannot locate the project's web files.", exception);
        }

        if (root == null) {
            throw new IllegalStateException(
                    "Cannot locate home.html. Run the application from the project or its target directory.");
        }
        return root;
    }

    private Path findWebRootFrom(Path location) {
        for (Path current = location; current != null; current = current.getParent()) {
            if (Files.isRegularFile(current.resolve("home.html"))
                    && Files.isRegularFile(current.resolve("css").resolve("style.css"))) {
                return current;
            }
        }
        return null;
    }
}
