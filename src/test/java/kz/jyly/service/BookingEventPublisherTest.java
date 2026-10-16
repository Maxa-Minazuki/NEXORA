package kz.jyly.service;

import kz.jyly.event.BookingEvent;
import kz.jyly.event.BookingEventType;
import kz.jyly.observer.BookingObserver;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Субъект (BookingEventPublisher) барлық тіркелген бақылаушыларды хабардар ететінін тексереді. */
class BookingEventPublisherTest {

    private static final BookingEvent EVENT = new BookingEvent(BookingEventType.BOOKING_CREATED, 1L,
            "JY-TEST-0001", "Focus Room", LocalDateTime.of(2026, 10, 20, 10, 0), LocalDateTime.of(2026, 10, 15, 9, 0));

    @AfterEach
    void clearSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void publisherNotifiesAllRegisteredObservers() {
        RecordingObserver first = new RecordingObserver();
        RecordingObserver second = new RecordingObserver();

        new BookingEventPublisher(List.of(first, second)).publish(EVENT);

        assertThat(first.received).containsExactly(EVENT);
        assertThat(second.received).containsExactly(EVENT);
    }

    @Test
    void failingObserverDoesNotStopOtherObservers() {
        RecordingObserver before = new RecordingObserver();
        BookingObserver failing = event -> {
            throw new IllegalStateException("observer down");
        };
        RecordingObserver after = new RecordingObserver();

        new BookingEventPublisher(List.of(before, failing, after)).publish(EVENT);

        assertThat(before.received).containsExactly(EVENT);
        assertThat(after.received).containsExactly(EVENT);
    }

    @Test
    void insideTransactionObserversAreNotifiedOnlyAfterCommit() {
        RecordingObserver observer = new RecordingObserver();
        TransactionSynchronizationManager.initSynchronization();

        new BookingEventPublisher(List.of(observer)).publish(EVENT);
        assertThat(observer.received).isEmpty();

        List<TransactionSynchronization> callbacks = TransactionSynchronizationManager.getSynchronizations();
        callbacks.forEach(TransactionSynchronization::afterCommit);

        assertThat(observer.received).containsExactly(EVENT);
    }

    @Test
    void rolledBackTransactionDoesNotNotifyObservers() {
        RecordingObserver observer = new RecordingObserver();
        TransactionSynchronizationManager.initSynchronization();

        new BookingEventPublisher(List.of(observer)).publish(EVENT);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertThat(observer.received).isEmpty();
    }

    static class RecordingObserver implements BookingObserver {
        final List<BookingEvent> received = new ArrayList<>();

        @Override
        public void onBookingEvent(BookingEvent event) {
            received.add(event);
        }
    }
}
