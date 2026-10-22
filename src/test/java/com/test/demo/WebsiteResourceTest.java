package com.test.demo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HomeController.class)
@Import(WebConfiguration.class)
class WebsiteResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootRedirectsToTheExistingHomePage() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/home.html"));
    }

    @Test
    void servesExistingHomePageAndStylesheetInPlace() throws Exception {
        mockMvc.perform(get("/home.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Our Courses")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("login-history.html")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("todo.html")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("profile.html")));

        mockMvc.perform(get("/css/style.css"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(".navbar-custom")));
    }

    @Test
    void servesTheAccountFeaturePagesAndScript() throws Exception {
        mockMvc.perform(get("/dashboard.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Welcome,")));

        mockMvc.perform(get("/profile.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Change password")));

        mockMvc.perform(get("/login-history.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Export Excel")));

        mockMvc.perform(get("/todo.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Add a task")));

        mockMvc.perform(get("/js/account.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("/api/account/todos")));

        mockMvc.perform(get("/js/config.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("apiBaseUrl")));
    }

    @Test
    void findsProjectFilesWhenStartedOutsideTheProjectDirectory() {
        String originalWorkingDirectory = System.getProperty("user.dir");
        Path expectedRoot = Path.of(originalWorkingDirectory).toAbsolutePath().normalize();
        try {
            System.setProperty("user.dir", System.getProperty("java.io.tmpdir"));
            assertThat(new WebConfiguration("http://localhost:8081").findWebRoot()).isEqualTo(expectedRoot);
        } finally {
            System.setProperty("user.dir", originalWorkingDirectory);
        }
    }
}
