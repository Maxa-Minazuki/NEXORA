package kz.jyly.service;

import kz.jyly.event.BookingEvent;
import kz.jyly.observer.BookingObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

/**
 * Observer үлгісіндегі «субъект» (Subject). Барлық BookingObserver бинтерін Spring өзі тіркейді.
 *
 * <p>Транзакция ішінде жарияланған оқиға бақылаушыларға тек транзакция сәтті commit болғаннан
 * кейін жеткізіледі: rollback болса, «жалған» хабарлама шықпайды. Бір бақылаушының қатесі
 * журналға жазылады, қалған бақылаушыларға және сақталған брондауға әсер етпейді.</p>
 */
@Component
public class BookingEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(BookingEventPublisher.class);

    private final List<BookingObserver> observers;

    public BookingEventPublisher(List<BookingObserver> observers) {
        this.observers = List.copyOf(observers);
    }

    public void publish(BookingEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    notifyObservers(event);
                }
            });
        } else {
            notifyObservers(event);
        }
    }

    void notifyObservers(BookingEvent event) {
        for (BookingObserver observer : observers) {
            try {
                observer.onBookingEvent(event);
            } catch (RuntimeException e) {
                log.error("Observer {} failed on {} for booking {}", observer.getClass().getSimpleName(),
                        event.type(), event.bookingNumber(), e);
            }
        }
    }

    public List<BookingObserver> getObservers() {
        return observers;
    }
}
