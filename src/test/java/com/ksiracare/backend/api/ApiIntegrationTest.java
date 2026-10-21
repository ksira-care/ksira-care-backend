package com.ksiracare.backend.api;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiIntegrationTest extends ApiTestSupport {

    @Test
    void signInSetsAnHttpOnlySessionCookieAndNoTokenInTheBody() throws Exception {
        MvcResult result = signIn("aapta@ksiracare.com", "password123");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Cookie cookie = result.getResponse().getCookie(COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api");
        assertThat(result.getResponse().getHeader("Set-Cookie")).contains("SameSite=Strict");
        assertThat(result.getResponse().getContentAsString())
                .contains("\"email\":\"aapta@ksiracare.com\"")
                .doesNotContain("token");
    }

    @Test
    void wrongPasswordAndUnknownEmailGiveTheSameAnswer() throws Exception {
        for (String email : new String[]{"aapta@ksiracare.com", "nobody@ksiracare.com"}) {
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
        mvc.perform(get("/api/aaptas/me").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/aaptas/me").contextPath("/api").cookie(new Cookie(COOKIE, "forged.token.value")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void theProfileComesFromTheSession() throws Exception {
        mvc.perform(get("/api/aaptas/me").contextPath("/api").cookie(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Aanya"))
                .andExpect(jsonPath("$.lastName").value("Mehta"))
                .andExpect(jsonPath("$.dateOfBirth").value("1991-03-14"))
                .andExpect(jsonPath("$.languages").isArray())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void theOldIdBasedProfileEndpointIsGone() throws Exception {
        mvc.perform(get("/api/aaptas/00000000-0000-0000-0000-000000000000").contextPath("/api").cookie(session()))
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
                .andExpect(jsonPath("$.bookings[0].aaptaFee").doesNotExist())
                .andExpect(jsonPath("$.aaptaName").doesNotExist());
    }

    @Test
    void theDashboardSummaryIsAvailable() throws Exception {
        mvc.perform(get("/api/aaptas/me/dashboard-summary").contextPath("/api").cookie(session()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAllTime").isNumber())
                .andExpect(jsonPath("$.completedThisMonth").isNumber());
    }

    @Test
    void theHealthCheckIsPublicAndRevealsNoDetails() throws Exception {
        mvc.perform(get("/api/actuator/health").contextPath("/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test
    void otherActuatorEndpointsAreNotExposed() throws Exception {
        mvc.perform(get("/api/actuator/env").contextPath("/api"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void signOutClearsTheCookie() throws Exception {
        mvc.perform(post("/api/auth/logout").contextPath("/api"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(COOKIE, 0));
    }

    @Test
    void slotsComeInAnEnvelope() throws Exception {
        long now = Instant.now().toEpochMilli();

        mvc.perform(get("/api/aaptas/me/slots").contextPath("/api").cookie(session())
                        .param("startTime", String.valueOf(now))
                        .param("endTime", String.valueOf(now + Duration.ofDays(7).toMillis())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slots").isNotEmpty())
                .andExpect(jsonPath("$.slots[0].time").isNumber());
    }

    @Test
    void reversedOrOversizedRangesAreRejected() throws Exception {
        long now = Instant.now().toEpochMilli();

        mvc.perform(get("/api/bookings").contextPath("/api").cookie(session())
                        .param("startTime", String.valueOf(now))
                        .param("endTime", String.valueOf(now - 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        mvc.perform(get("/api/aaptas/me/slots").contextPath("/api").cookie(session())
                        .param("startTime", "0")
                        .param("endTime", String.valueOf(now)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void unknownStatusValuesAreRejected() throws Exception {
        mvc.perform(patch("/api/bookings/00000000-0000-0000-0000-000000000000").contextPath("/api").cookie(session())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bookingStatus\":\"THERAPIST_NO_SHOW\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    // ── Admin ─────────────────────────────────────────────────

    @Test
    void adminSignInSetsItsOwnCookieScopedToTheAdminApi() throws Exception {
        MvcResult result = adminSignIn("admin@ksiracare.com", "password123");

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        Cookie cookie = result.getResponse().getCookie(ADMIN_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/api/admin");
        assertThat(result.getResponse().getCookie(COOKIE)).as("no aapta session").isNull();
        assertThat(result.getResponse().getContentAsString())
                .contains("\"adminId\"")
                .contains("\"email\":\"admin@ksiracare.com\"")
                .doesNotContain("token");
    }

    @Test
    void theAdminProfileComesFromTheAdminSession() throws Exception {
        mvc.perform(get("/api/admin/me").contextPath("/api").cookie(adminSession()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("admin@ksiracare.com"))
                .andExpect(jsonPath("$.firstName").value("Meera"))
                .andExpect(jsonPath("$.password").doesNotExist());
        mvc.perform(get("/api/admin/me").contextPath("/api"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void aaptaAndAdminAccountsAndSessionsDontCross() throws Exception {
        // Each account list only answers its own sign-in endpoint.
        assertThat(adminSignIn("aapta@ksiracare.com", "password123").getResponse().getStatus()).isEqualTo(401);
        assertThat(signIn("admin@ksiracare.com", "password123").getResponse().getStatus()).isEqualTo(401);

        // An aapta session doesn't open the admin API, and vice versa — even if the token
        // is moved into the other cookie.
        Cookie aapta = session();
        Cookie admin = adminSession();
        mvc.perform(get("/api/admin/me").contextPath("/api").cookie(aapta))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/admin/me").contextPath("/api").cookie(new Cookie(ADMIN_COOKIE, aapta.getValue())))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/aaptas/me").contextPath("/api").cookie(admin))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/aaptas/me").contextPath("/api").cookie(new Cookie(COOKIE, admin.getValue())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminSignOutClearsOnlyTheAdminCookie() throws Exception {
        mvc.perform(post("/api/admin/auth/logout").contextPath("/api"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge(ADMIN_COOKIE, 0))
                .andExpect(cookie().path(ADMIN_COOKIE, "/api/admin"))
                .andExpect(cookie().doesNotExist(COOKIE));
    }
}
