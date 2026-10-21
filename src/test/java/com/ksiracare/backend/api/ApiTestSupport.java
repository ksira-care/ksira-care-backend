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
    static final String ADMIN_COOKIE = "ksira_admin_session";
    static final String DEMO_PASSWORD = "password123";

    @Autowired
    private WebApplicationContext context;
    MockMvc mvc;

    @BeforeEach
    void setUpMockMvc() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    MvcResult signIn(String email, String password) throws Exception {
        return login("/api/auth/login", email, password);
    }

    MvcResult adminSignIn(String email, String password) throws Exception {
        return login("/api/admin/auth/login", email, password);
    }

    /** A session cookie for the demo aapta. */
    Cookie session() throws Exception {
        return session("aapta@ksiracare.com");
    }

    Cookie session(String aaptaEmail) throws Exception {
        return signIn(aaptaEmail, DEMO_PASSWORD).getResponse().getCookie(COOKIE);
    }

    /** A session cookie for the demo admin. */
    Cookie adminSession() throws Exception {
        return adminSignIn("admin@ksiracare.com", DEMO_PASSWORD).getResponse().getCookie(ADMIN_COOKIE);
    }

    private MvcResult login(String path, String email, String password) throws Exception {
        return mvc.perform(post(path).contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn();
    }
}
