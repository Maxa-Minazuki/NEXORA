package kz.jyly.observer;

import kz.jyly.event.BookingEvent;

/**
 * Observer үлгісіндегі бақылаушы интерфейсі.
 * Жаңа бақылаушы қосу үшін осы интерфейсті іске асыратын @Component жасау жеткілікті —
 * BookingService пен PaymentService кодын өзгерту қажет емес.
 */
public interface BookingObserver {

    void onBookingEvent(BookingEvent event);
}
