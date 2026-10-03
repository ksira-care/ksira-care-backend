package com.ksiracare.backend.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the real application (dev profile: in-memory database, Flyway migrations, demo data)
 * through the security filters, the way the portal does.
 */
@SpringBootTest
class TherapistPortalApiIntegrationTest {

    private static final String COOKIE = "ksira_session";

    @Autowired
    private WebApplicationContext context;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private MvcResult signIn(String email, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andReturn();
    }

    private Cookie session() throws Exception {
        return signIn("therapist@ksiracare.com", "password123").getResponse().getCookie(COOKIE);
    }

    @Test
    void signInSetsAnHttpOnlySessionCookieAndNoTokenInTheBody() throws Exception {
        MvcResult result = signIn("therapist@ksiracare.com", "password123");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Cookie cookie = result.getResponse().getCookie(COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict");
        assertThat(result.getResponse().getContentAsString())
                .contains("\"email\":\"therapist@ksiracare.com\"")
                .doesNotContain("token");
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
        for (String email : new String[]{"therapist@ksiracare.com", "nobody@ksiracare.com"}) {
            MvcResult result = signIn(email, "wrong-password");
            assertThat(result.getResponse().getStatus()).isEqualTo(401);
            assertThat(result.getResponse().getContentAsString()).contains("\"code\":\"INVALID_CREDENTIALS\"");
        }
    }

    @Test
    void aDisabledAccountCannotSignIn() throws Exception {
        MvcResult result = signIn("disabled@ksiracare.com", "password123");

        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(result.getResponse().getContentAsString()).contains("\"code\":\"ACCOUNT_DISABLED\"");
    }

    @Test
    void withoutAValidSessionEverythingAnswers401() throws Exception {
        mvc.perform(get("/api/therapists/me").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/therapists/me").contextPath("/api").cookie(new Cookie(COOKIE, "forged.token.value")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theProfileComesFromTheSession() throws Exception {
        mvc.perform(get("/api/therapists/me").contextPath("/api").cookie(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Aanya"))
                .andExpect(jsonPath("$.lastName").value("Mehta"))
                .andExpect(jsonPath("$.dateOfBirth").value("1991-03-14"))
                .andExpect(jsonPath("$.languages").isArray())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void theOldIdBasedProfileEndpointIsGone() throws Exception {
        mvc.perform(get("/api/therapists/00000000-0000-0000-0000-000000000000").contextPath("/api").cookie(session()))
                .andExpect(status().isNotFound());
    }

    @Test
    void bookingsAreScopedToTheSessionAndHideInternalFields() throws Exception {
        long now = Instant.now().toEpochMilli();
        long monthAgo = Instant.now().minus(30, ChronoUnit.DAYS).toEpochMilli();

        mvc.perform(get("/api/bookings").contextPath("/api").cookie(session())
                        .param("startTime", String.valueOf(monthAgo))
                        .param("endTime", String.valueOf(now)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookings").isNotEmpty())
                .andExpect(jsonPath("$.bookings[0].customerName").exists())
                .andExpect(jsonPath("$.bookings[0].customerCountry").doesNotExist())
                .andExpect(jsonPath("$.bookings[0].therapistFee").doesNotExist())
                .andExpect(jsonPath("$.therapistName").doesNotExist());
    }

    @Test
    void theDashboardSummaryIsAvailable() throws Exception {
        mvc.perform(get("/api/therapists/me/dashboard-summary").contextPath("/api").cookie(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAllTime").isNumber())
                .andExpect(jsonPath("$.completedThisMonth").isNumber());
    }

    @Test
    void signOutClearsTheCookie() throws Exception {
        mvc.perform(post("/api/auth/logout").contextPath("/api"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(COOKIE, 0));
    }
}
