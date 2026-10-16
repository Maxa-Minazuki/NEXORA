package kz.jyly.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Тіркелусіз клиенттің өз брондауларына қолжетімділігі.
 * Қолжетімділік тек HTTP-сессияда және тек нақты брондау нөміріне беріледі:
 * брондауды жасағанда немесе нөмір + электрондық пошта арқылы растағанда.
 * Тек ID немесе тек нөмір бойынша бөтен брондауды ашу мүмкін емес.
 */
@Service
public class BookingAccessService {

    private static final String SESSION_KEY = "jyly.grantedBookings";

    public void grant(HttpSession session, String bookingNumber) {
        granted(session).add(BookingService.normalizeNumber(bookingNumber));
    }

    public boolean hasAccess(HttpSession session, String bookingNumber) {
        return bookingNumber != null && granted(session).contains(BookingService.normalizeNumber(bookingNumber));
    }

    public Set<String> grantedNumbers(HttpSession session) {
        return Collections.unmodifiableSet(granted(session));
    }

    public void forget(HttpSession session) {
        session.removeAttribute(SESSION_KEY);
    }

    @SuppressWarnings("unchecked")
    private Set<String> granted(HttpSession session) {
        Object value = session.getAttribute(SESSION_KEY);
        if (value instanceof Set<?> set) {
            return (Set<String>) set;
        }
        Set<String> created = new LinkedHashSet<>();
        session.setAttribute(SESSION_KEY, created);
        return created;
    }
}
