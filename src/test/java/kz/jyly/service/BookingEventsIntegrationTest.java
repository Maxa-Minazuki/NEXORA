package kz.jyly.service;

import kz.jyly.dto.BookingForm;
import kz.jyly.dto.PaymentForm;
import kz.jyly.entity.Booking;
import kz.jyly.entity.Room;
import kz.jyly.enums.BookingStatus;
import kz.jyly.enums.DemoOutcome;
import kz.jyly.enums.PaymentType;
import kz.jyly.enums.RoomType;
import kz.jyly.event.BookingEvent;
import kz.jyly.event.BookingEventType;
import kz.jyly.exception.BookingConflictException;
import kz.jyly.observer.BookingAuditObserver;
import kz.jyly.observer.BookingNotificationObserver;
import kz.jyly.observer.BookingObserver;
import kz.jyly.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Нақты брондау әрекеттері күтілетін оқиғаларды жариялайтынын тексереді.
 * Тестке арналған бақылаушы жай ғана @Bean ретінде қосылады — сервистер кодын өзгертпей.
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingEventsIntegrationTest {

    @TestConfiguration
    static class RecordingConfig {
        @Bean
        RecordingObserver recordingObserver() {
            return new RecordingObserver();
        }
    }

    static class RecordingObserver implements BookingObserver {
        final List<BookingEvent> events = new CopyOnWriteArrayList<>();

        @Override
        public void onBookingEvent(BookingEvent event) {
            events.add(event);
        }

        List<BookingEventType> typesFor(String bookingNumber) {
            return events.stream().filter(e -> e.bookingNumber().equals(bookingNumber))
                    .map(BookingEvent::type).toList();
        }
    }

    @Autowired BookingService bookingService;
    @Autowired PaymentService paymentService;
    @Autowired RoomRepository roomRepository;
    @Autowired BookingRules rules;
    @Autowired BookingEventPublisher publisher;
    @Autowired RecordingObserver recorder;

    private Room room;
    private LocalDate day;

    @BeforeEach
    void setUp() {
        recorder.events.clear();
        Room r = new Room();
        r.setName("Observer Room");
        r.setSlug("observer-" + UUID.randomUUID());
        r.setType(RoomType.NETWORKING);
        r.setShortDescription("Тест");
        r.setDescription("Observer тест бөлмесі");
        r.setCapacity(8);
        r.setArea(30);
        r.setHourlyRate(new BigDecimal("12000"));
        r.setActive(true);
        room = roomRepository.save(r);
        day = rules.today().plusDays(10);
    }

    @Test
    void springRegistersBothConcreteObservers() {
        assertThat(publisher.getObservers())
                .hasAtLeastOneElementOfType(BookingAuditObserver.class)
                .hasAtLeastOneElementOfType(BookingNotificationObserver.class)
                .hasAtLeastOneElementOfType(RecordingObserver.class);
    }

    @Test
    void creatingBookingPublishesCreatedEventAfterCommit() {
        Booking b = bookingService.create(form("10:00", "12:00"));

        assertThat(recorder.typesFor(b.getBookingNumber())).containsExactly(BookingEventType.BOOKING_CREATED);
        BookingEvent event = recorder.events.get(0);
        assertThat(event.bookingId()).isEqualTo(b.getId());
        assertThat(event.roomName()).isEqualTo("Observer Room");
    }

    @Test
    void rejectedBookingPublishesNothing() {
        bookingService.create(form("10:00", "12:00"));
        recorder.events.clear();

        assertThatThrownBy(() -> bookingService.create(form("11:00", "13:00")))
                .isInstanceOf(BookingConflictException.class);

        assertThat(recorder.events).isEmpty();
    }

    @Test
    void paidDepositPublishesConfirmedEvent() {
        Booking b = bookingService.create(form("14:00", "16:00"));

        paymentService.pay(b.getId(), payment(DemoOutcome.SUCCESS));

        assertThat(recorder.typesFor(b.getBookingNumber()))
                .containsExactly(BookingEventType.BOOKING_CREATED, BookingEventType.BOOKING_CONFIRMED);
    }

    @Test
    void failedPaymentDoesNotPublishConfirmedEvent() {
        Booking b = bookingService.create(form("14:00", "16:00"));

        paymentService.pay(b.getId(), payment(DemoOutcome.FAILURE));

        assertThat(recorder.typesFor(b.getBookingNumber())).containsExactly(BookingEventType.BOOKING_CREATED);
    }

    @Test
    void adminConfirmAndCancelPublishEvents() {
        Booking b = bookingService.create(form("17:00", "19:00"));

        bookingService.changeStatusByAdmin(b.getId(), BookingStatus.CONFIRMED);
        bookingService.changeStatusByAdmin(b.getId(), BookingStatus.CANCELLED);

        assertThat(recorder.typesFor(b.getBookingNumber())).containsExactly(
                BookingEventType.BOOKING_CREATED, BookingEventType.BOOKING_CONFIRMED, BookingEventType.BOOKING_CANCELLED);
    }

    @Test
    void customerCancellationPublishesCancelledEvent() {
        Booking b = bookingService.create(form("08:00", "09:00"));

        bookingService.cancelByCustomer(b.getBookingNumber());

        assertThat(recorder.typesFor(b.getBookingNumber()))
                .containsExactly(BookingEventType.BOOKING_CREATED, BookingEventType.BOOKING_CANCELLED);
    }

    private BookingForm form(String start, String end) {
        BookingForm f = new BookingForm();
        f.setRoomId(room.getId());
        f.setDate(day);
        f.setStartTime(LocalTime.parse(start));
        f.setEndTime(LocalTime.parse(end));
        f.setGuestCount(2);
        f.setPurpose("Observer тесті");
        f.setFirstName("Тест");
        f.setLastName("Ұйымдастырушы");
        f.setPhone("+7 701 000 00 00");
        f.setEmail("observer@example.kz");
        return f;
    }

    private static PaymentForm payment(DemoOutcome outcome) {
        PaymentForm p = new PaymentForm();
        p.setType(PaymentType.DEPOSIT);
        p.setOutcome(outcome);
        p.setAcknowledgeDemo(true);
        p.setIdempotencyKey(UUID.randomUUID().toString());
        return p;
    }
}
