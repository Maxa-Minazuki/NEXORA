package kz.jyly.observer;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import kz.jyly.event.BookingEvent;
import kz.jyly.event.BookingEventType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Әр нақты бақылаушы өзіне келген оқиғаны өңдейтінін журнал жазбасы арқылы тексереді. */
class BookingObserversTest {

    private final List<Runnable> cleanup = new ArrayList<>();

    @AfterEach
    void restoreLoggers() {
        cleanup.forEach(Runnable::run);
    }

    @Test
    void auditObserverLogsEventTypeAndBookingNumber() {
        ListAppender<ILoggingEvent> logs = capture(BookingAuditObserver.class);

        new BookingAuditObserver().onBookingEvent(event(BookingEventType.BOOKING_CREATED));

        assertThat(logs.list).singleElement().satisfies(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.INFO);
            assertThat(e.getFormattedMessage())
                    .contains("[AUDIT]", "BOOKING_CREATED", "JY-TEST-0001", "Connect Room");
        });
    }

    @Test
    void notificationObserverWritesClearlyLabelledDemoMessage() {
        ListAppender<ILoggingEvent> logs = capture(BookingNotificationObserver.class);

        new BookingNotificationObserver().onBookingEvent(event(BookingEventType.BOOKING_CONFIRMED));

        assertThat(logs.list).singleElement().satisfies(e -> assertThat(e.getFormattedMessage())
                .contains("[DEMO NOTIFICATION]", "JY-TEST-0001", "расталды", "нақты email/SMS жіберілмеді"));
    }

    @Test
    void notificationObserverHasMessageForEveryEventType() {
        BookingNotificationObserver observer = new BookingNotificationObserver();
        for (BookingEventType type : BookingEventType.values()) {
            assertThat(observer.message(event(type))).isNotBlank();
        }
    }

    private static BookingEvent event(BookingEventType type) {
        LocalDateTime start = LocalDateTime.of(2026, 10, 20, 10, 0);
        return new BookingEvent(type, 42L, "JY-TEST-0001", "Connect Room", start, start.minusDays(1));
    }

    private ListAppender<ILoggingEvent> capture(Class<?> type) {
        Logger logger = (Logger) LoggerFactory.getLogger(type);
        Level previous = logger.getLevel();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        logger.setLevel(Level.INFO); // test профилінде kz.jyly деңгейі WARN
        cleanup.add(() -> {
            logger.detachAppender(appender);
            logger.setLevel(previous);
        });
        return appender;
    }
}
