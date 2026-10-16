package kz.jyly.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** HTTP маршруттары, CSRF және әкімші бөлімін қорғау. Демо деректер (seed) тест профилінде де жүктеледі. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WebAndSecurityTest {

    @Autowired
    MockMvc mvc;

    @Test
    void publicPagesAreAvailable() throws Exception {
        for (String url : new String[]{"/", "/rooms", "/rooms/connect-room", "/about", "/bookings/new", "/my-bookings", "/login"}) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    void homePageIsInKazakhAndListsRoomsFromDatabase() throws Exception {
        mvc.perform(get("/"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Маңызды кездесулерге арналған")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Executive Room")));
    }

    @Test
    void catalogFiltersByCapacityAndType() throws Exception {
        mvc.perform(get("/rooms").param("guests", "20"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Studio Room")))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Focus Room</a>"))));
        mvc.perform(get("/rooms").param("type", "EXECUTIVE"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("1 бөлме табылды")));
    }

    @Test
    void unknownRoomReturns404Page() throws Exception {
        mvc.perform(get("/rooms/no-such-room")).andExpect(status().isNotFound());
    }

    @Test
    void pricingApiReturnsServerCalculatedQuote() throws Exception {
        String date = LocalDate.now().plusDays(20).toString();
        mvc.perform(get("/api/pricing/quote").param("roomId", "2").param("date", date)
                        .param("start", "15:00").param("end", "18:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.billableHours").value(3))
                .andExpect(jsonPath("$.total").value("36 000 ₸"))
                .andExpect(jsonPath("$.deposit").value("10 800 ₸"));
    }

    @Test
    void adminRequiresLogin() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
        mvc.perform(get("/admin/rooms")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/bookings")).andExpect(status().is3xxRedirection());
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminUserIsForbidden() throws Exception {
        mvc.perform(get("/admin")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/rooms/1/toggle").with(csrf())).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPagesRenderForAdmin() throws Exception {
        for (String url : new String[]{"/admin", "/admin/rooms", "/admin/rooms/new", "/admin/rooms/1/edit",
                "/admin/bookings", "/admin/customers", "/admin/payments"}) {
            mvc.perform(get(url)).andExpect(status().isOk());
        }
    }

    @Test
    void configuredAdminCanLogInWithHashedPassword() throws Exception {
        mvc.perform(formLogin("/login").user("admin").password("test-admin-password"))
                .andExpect(redirectedUrl("/admin"));
        mvc.perform(formLogin("/login").user("admin").password("wrong"))
                .andExpect(redirectedUrl("/login?error"));
    }

    @Test
    void postWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/bookings").param("roomId", "1")).andExpect(status().isForbidden());
    }

    @Test
    void bookingDetailsRequireVerifiedAccess() throws Exception {
        mvc.perform(get("/my-bookings/JY-DEMO-2026"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/my-bookings"));

        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/my-bookings/lookup").session(session).with(csrf())
                        .param("bookingNumber", "JY-DEMO-2026").param("email", "wrong@example.kz"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Брондау табылмады")));

        mvc.perform(post("/my-bookings/lookup").session(session).with(csrf())
                        .param("bookingNumber", "JY-DEMO-2026").param("email", "aigerim@example.kz"))
                .andExpect(redirectedUrl("/my-bookings/JY-DEMO-2026"));
        mvc.perform(get("/my-bookings/JY-DEMO-2026").session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Executive Room")));
    }

    @Test
    void bookingFormShowsServerValidationErrorsInKazakh() throws Exception {
        MvcResult result = mvc.perform(post("/bookings").with(csrf())
                        .param("roomId", "1").param("date", LocalDate.now().plusDays(15).toString())
                        .param("startTime", "10:00").param("endTime", "11:00").param("guestCount", "0")
                        .param("purpose", "").param("firstName", "A").param("lastName", "B")
                        .param("phone", "abc").param("email", "not-an-email"))
                .andExpect(status().isOk())
                .andReturn();
        String html = result.getResponse().getContentAsString();
        assertThat(html).contains("Қонақтар саны нөлден көп болуы керек", "Телефон нөмірі дұрыс емес",
                "Электрондық пошта дұрыс емес", "Кездесу мақсатын көрсетіңіз");
    }

    @Test
    void validBookingPostCreatesBookingAndRedirectsToDetails() throws Exception {
        MockHttpSession session = new MockHttpSession();
        MvcResult result = mvc.perform(post("/bookings").session(session).with(csrf())
                        .param("roomId", "3").param("date", LocalDate.now().plusDays(30).toString())
                        .param("startTime", "10:00").param("endTime", "12:00").param("guestCount", "10")
                        .param("purpose", "Воркшоп").param("firstName", "Аружан").param("lastName", "Сәрсен")
                        .param("phone", "+7 701 555 44 33").param("email", "aruzhan@example.kz")
                        .param("guests[0].firstName", "Мақсат").param("guests[0].lastName", "Әлиев"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = result.getResponse().getRedirectedUrl();
        assertThat(location).startsWith("/my-bookings/JY-");
        mvc.perform(get(location).session(session))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Мақсат Әлиев")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Күтілуде")));
    }
}
