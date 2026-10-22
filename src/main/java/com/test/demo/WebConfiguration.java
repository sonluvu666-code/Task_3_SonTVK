package com.test.demo;

import java.nio.file.Path;
import java.nio.file.Files;
import java.net.URISyntaxException;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {

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
