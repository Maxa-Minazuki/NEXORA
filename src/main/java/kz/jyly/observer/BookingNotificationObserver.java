package kz.jyly.observer;

import kz.jyly.event.BookingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Клиентке хабарлама жіберудің ДЕМО нұсқасы. Жобада email/SMS инфрақұрылымы жоқ,
 * сондықтан хабарлама тек журналға жазылады — нақты хат немесе SMS жіберілмейді.
 */
@Component
public class BookingNotificationObserver implements BookingObserver {

    private static final Logger log = LoggerFactory.getLogger(BookingNotificationObserver.class);

    @Override
    public void onBookingEvent(BookingEvent event) {
        log.info("[DEMO NOTIFICATION] booking={}: {} (нақты email/SMS жіберілмеді)",
                event.bookingNumber(), message(event));
    }

    String message(BookingEvent event) {
        return switch (event.type()) {
            case BOOKING_CREATED -> "Брондауыңыз қабылданды. Растау үшін депозитті төлеңіз";
            case BOOKING_CONFIRMED -> "Брондауыңыз расталды. " + event.roomName() + " сізді күтеді";
            case BOOKING_CANCELLED -> "Брондауыңыз тоқтатылды";
        };
    }
}
