package kz.jyly.event;

import kz.jyly.entity.Booking;

import java.time.LocalDateTime;

/**
 * Брондау оқиғасы (Observer үлгісіндегі «хабарлама»).
 * Сущностың өзі емес, оның өзгермейтін көшірмесі беріледі: бақылаушылар транзакция
 * аяқталғаннан кейін шақырылады және брондауды өзгерте алмайды.
 * Клиенттің жеке деректері (аты, поштасы, телефоны) әдейі берілмейді.
 */
public record BookingEvent(
        BookingEventType type,
        Long bookingId,
        String bookingNumber,
        String roomName,
        LocalDateTime startDateTime,
        LocalDateTime occurredAt) {

    public static BookingEvent of(BookingEventType type, Booking booking, LocalDateTime occurredAt) {
        return new BookingEvent(type, booking.getId(), booking.getBookingNumber(),
                booking.getRoom().getName(), booking.getStartDateTime(), occurredAt);
    }
}
