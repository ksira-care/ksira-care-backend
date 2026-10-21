package com.ksiracare.backend.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Drives the real application (dev profile: in-memory database, Flyway migrations, demo data)
 * through the security filters, the way the aapta portal and admin console do. Paths include
 * the {@code /api} context path.
 */
@SpringBootTest
abstract class ApiTestSupport {

    static final String COOKIE = "ksira_session";
    static final String DEMO_PASSWORD = "password123";

    @Autowired
    private WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** Aaptas and admins sign in the same way. */
    MvcResult signIn(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn();
    }

    /** A session cookie for the demo aapta. */
    Cookie session() throws Exception {
        return session("aapta@ksiracare.com");
    }

    /** A session cookie for any account, aapta or admin, with the demo password. */
    Cookie session(String email) throws Exception {
        return signIn(email, DEMO_PASSWORD).getResponse().getCookie(COOKIE);
    }

    /** A session cookie for the demo admin. */
    Cookie adminSession() throws Exception {
        return session("admin@ksiracare.com");
    }
}
