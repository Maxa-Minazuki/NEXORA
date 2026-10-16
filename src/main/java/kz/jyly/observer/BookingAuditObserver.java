package kz.jyly.observer;

import kz.jyly.event.BookingEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Аудит журналы: брондаудың әр өмірлік цикл оқиғасын тіркейді (жеке деректерсіз). */
@Component
public class BookingAuditObserver implements BookingObserver {

    private static final Logger log = LoggerFactory.getLogger(BookingAuditObserver.class);

    @Override
    public void onBookingEvent(BookingEvent event) {
        log.info("[AUDIT] {} booking={} id={} room={} start={} at={}",
                event.type(), event.bookingNumber(), event.bookingId(), event.roomName(),
                event.startDateTime(), event.occurredAt());
    }
}
