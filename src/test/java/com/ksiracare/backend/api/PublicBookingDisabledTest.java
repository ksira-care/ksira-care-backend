package com.ksiracare.backend.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** With public booking switched off (production until the site opens), the endpoints don't exist. */
@SpringBootTest(properties = "ksira.public-booking.enabled=false")
class PublicBookingDisabledTest extends ApiTestSupport {

    @Test
    void theBookingEndpointsAnswer404() throws Exception {
        mvc.perform(get("/api/public/availability").contextPath("/api")
                        .param("startTime", "0").param("endTime", "1").param("languages", "en"))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/public/bookings").contextPath("/api")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isNotFound());
    }
}
