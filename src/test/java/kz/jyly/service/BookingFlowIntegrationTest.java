package kz.jyly.service;

import kz.jyly.dto.BookingForm;
import kz.jyly.dto.GuestForm;
import kz.jyly.dto.PaymentForm;
import kz.jyly.entity.Booking;
import kz.jyly.entity.Payment;
import kz.jyly.entity.Room;
import kz.jyly.enums.BookingStatus;
import kz.jyly.enums.DemoOutcome;
import kz.jyly.enums.PaymentStatus;
import kz.jyly.enums.PaymentType;
import kz.jyly.enums.RoomType;
import kz.jyly.enums.TransactionStatus;
import kz.jyly.exception.BookingConflictException;
import kz.jyly.exception.BusinessException;
import kz.jyly.repository.BookingRepository;
import kz.jyly.repository.GuestRepository;
import kz.jyly.repository.PaymentRepository;
import kz.jyly.repository.RoomRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Нақты Spring контексі және H2 дерекқоры бар интеграциялық тесттер.
 * Әр тест өз бөлмесін жасайды, сондықтан тесттер бір-біріне әсер етпейді.
 */
@SpringBootTest
@ActiveProfiles("test")
class BookingFlowIntegrationTest {

    @Autowired BookingService bookingService;
    @Autowired PaymentService paymentService;
    @Autowired RoomRepository roomRepository;
    @Autowired BookingRepository bookingRepository;
    @Autowired PaymentRepository paymentRepository;
    @Autowired GuestRepository guestRepository;
    @Autowired BookingRules rules;

    private Room room;
    private LocalDate day;

    @BeforeEach
    void createRoom() {
        Room r = new Room();
        r.setName("Test Room");
        r.setSlug("test-" + UUID.randomUUID());
        r.setType(RoomType.NETWORKING);
        r.setShortDescription("Тест");
        r.setDescription("Тест бөлмесі");
        r.setCapacity(8);
        r.setArea(30);
        r.setHourlyRate(new BigDecimal("12000"));
        r.setActive(true);
        room = roomRepository.save(r);
        day = rules.today().plusDays(10);
    }

    @Test
    void createsPendingBookingWithServerSidePriceAndGuests() {
        BookingForm form = form("10:00", "13:00", 3);
        form.getGuests().add(new GuestForm("Әлия", "Бекова", null, null));
        form.getGuests().add(new GuestForm("Нұр", "Ахмет", "nur@example.kz", "ACME"));

        Booking b = bookingService.create(form);

        assertThat(b.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(b.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(b.getTotalAmount()).isEqualByComparingTo("36000");
        assertThat(b.getDepositAmount()).isEqualByComparingTo("10800");
        assertThat(b.getBookingNumber()).matches("JY-[A-Z2-9]{4}-[A-Z2-9]{4}");
        assertThat(guestRepository.findByBookingIdOrderByIdAsc(b.getId())).extracting("firstName")
                .containsExactly("Әлия", "Нұр");
    }

    @Test
    void overlappingBookingIsRejected() {
        bookingService.create(form("10:00", "13:00", 2));

        assertThatThrownBy(() -> bookingService.create(form("12:00", "14:00", 2)))
                .isInstanceOf(BookingConflictException.class)
                .hasMessageContaining("бос емес");
        assertThatThrownBy(() -> bookingService.create(form("09:00", "10:30", 2)))
                .isInstanceOf(BookingConflictException.class);
        assertThatThrownBy(() -> bookingService.create(form("10:30", "11:30", 2)))
                .isInstanceOf(BookingConflictException.class);
    }

    @Test
    void adjacentIntervalsDoNotOverlap() {
        bookingService.create(form("10:00", "12:00", 2));

        Booking before = bookingService.create(form("08:00", "10:00", 2));
        Booking after = bookingService.create(form("12:00", "13:00", 2));

        assertThat(before.getId()).isNotNull();
        assertThat(after.getId()).isNotNull();
    }

    @Test
    void slotCanBeBookedAgainAfterCancellation() {
        Booking first = bookingService.create(form("15:00", "17:00", 2));
        bookingService.cancelByCustomer(first.getBookingNumber());

        Booking second = bookingService.create(form("15:00", "17:00", 2));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(bookingService.getByNumber(first.getBookingNumber()).getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void capacityIsEnforcedOnServer() {
        assertThatThrownBy(() -> bookingService.create(form("10:00", "11:00", 9)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("сыйымдылығынан (8 адам)");
    }

    @Test
    void namedGuestsCannotExceedGuestCount() {
        BookingForm form = form("10:00", "11:00", 1);
        form.getGuests().add(new GuestForm("А", "Б", null, null));
        form.getGuests().add(new GuestForm("В", "Г", null, null));

        assertThatThrownBy(() -> bookingService.create(form)).isInstanceOf(BusinessException.class);
    }

    @Test
    void inactiveRoomCannotBeBooked() {
        room.setActive(false);
        roomRepository.save(room);

        assertThatThrownBy(() -> bookingService.create(form("10:00", "11:00", 2)))
                .hasMessageContaining("қолжетімсіз");
    }

    @Test
    void pastDateIsRejectedByBookingService() {
        day = rules.today().minusDays(1);
        assertThatThrownBy(() -> bookingService.create(form("10:00", "11:00", 2)))
                .hasMessageContaining("Өткен күнге");
    }

    @Test
    void successfulDepositIsCountedAndConfirmsBooking() {
        Booking b = bookingService.create(form("10:00", "13:00", 2));

        Payment p = paymentService.pay(b.getId(), payment(PaymentType.DEPOSIT, null, DemoOutcome.SUCCESS));

        Booking reloaded = bookingService.getById(b.getId());
        assertThat(p.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
        assertThat(p.getAmount()).isEqualByComparingTo("10800");
        assertThat(reloaded.getAmountPaid()).isEqualByComparingTo("10800");
        assertThat(reloaded.getRemainingAmount()).isEqualByComparingTo("25200");
        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        assertThat(reloaded.getPaymentStatus()).isEqualTo(PaymentStatus.DEPOSIT_PAID);
    }

    @Test
    void failedPaymentIsStoredButNotCounted() {
        Booking b = bookingService.create(form("10:00", "13:00", 2));

        Payment p = paymentService.pay(b.getId(), payment(PaymentType.DEPOSIT, null, DemoOutcome.FAILURE));

        Booking reloaded = bookingService.getById(b.getId());
        assertThat(p.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(paymentRepository.findById(p.getId())).isPresent();
        assertThat(reloaded.getAmountPaid()).isEqualByComparingTo("0");
        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.PENDING);
        assertThat(reloaded.getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }

    @Test
    void overpaymentIsRejected() {
        Booking b = bookingService.create(form("10:00", "13:00", 2));
        paymentService.pay(b.getId(), payment(PaymentType.DEPOSIT, null, DemoOutcome.SUCCESS));

        assertThatThrownBy(() -> paymentService.pay(b.getId(), payment(PaymentType.BALANCE, new BigDecimal("25201"), DemoOutcome.SUCCESS)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("аспауы керек");
        assertThat(bookingService.getById(b.getId()).getAmountPaid()).isEqualByComparingTo("10800");
    }

    @Test
    void negativeOrZeroAmountsAreRejected() {
        Booking b = bookingService.create(form("10:00", "13:00", 2));
        paymentService.pay(b.getId(), payment(PaymentType.DEPOSIT, null, DemoOutcome.SUCCESS));

        assertThatThrownBy(() -> paymentService.pay(b.getId(), payment(PaymentType.BALANCE, new BigDecimal("-5"), DemoOutcome.SUCCESS)))
                .hasMessageContaining("нөлден көп");
        assertThatThrownBy(() -> paymentService.pay(b.getId(), payment(PaymentType.BALANCE, BigDecimal.ZERO, DemoOutcome.SUCCESS)))
                .hasMessageContaining("нөлден көп");
    }

    @Test
    void balanceRequiresDepositFirstAndDepositCannotBePaidTwice() {
        Booking b = bookingService.create(form("10:00", "13:00", 2));

        assertThatThrownBy(() -> paymentService.pay(b.getId(), payment(PaymentType.BALANCE, new BigDecimal("1000"), DemoOutcome.SUCCESS)))
                .hasMessageContaining("Алдымен депозитті");

        paymentService.pay(b.getId(), payment(PaymentType.DEPOSIT, null, DemoOutcome.SUCCESS));
        assertThatThrownBy(() -> paymentService.pay(b.getId(), payment(PaymentType.DEPOSIT, null, DemoOutcome.SUCCESS)))
                .hasMessageContaining("бұрын төленген");
    }

    @Test
    void sameIdempotencyKeyIsCountedOnlyOnce() {
        Booking b = bookingService.create(form("10:00", "13:00", 2));
        PaymentForm form = payment(PaymentType.DEPOSIT, null, DemoOutcome.SUCCESS);

        Payment first = paymentService.pay(b.getId(), form);
        Payment second = paymentService.pay(b.getId(), form);

        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(paymentService.history(b.getId())).hasSize(1);
        assertThat(bookingService.getById(b.getId()).getAmountPaid()).isEqualByComparingTo("10800");
    }

    @Test
    void partialAndFullPaymentStatuses() {
        Booking b = bookingService.create(form("10:00", "13:00", 2));
        paymentService.pay(b.getId(), payment(PaymentType.DEPOSIT, null, DemoOutcome.SUCCESS));
        paymentService.pay(b.getId(), payment(PaymentType.BALANCE, new BigDecimal("5000"), DemoOutcome.SUCCESS));

        assertThat(bookingService.getById(b.getId()).getPaymentStatus()).isEqualTo(PaymentStatus.PARTIALLY_PAID);

        paymentService.pay(b.getId(), payment(PaymentType.BALANCE, new BigDecimal("20200"), DemoOutcome.SUCCESS));
        Booking paid = bookingService.getById(b.getId());
        assertThat(paid.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);
        assertThat(paid.getRemainingAmount()).isEqualByComparingTo("0");
        assertThatThrownBy(() -> paymentService.pay(b.getId(), payment(PaymentType.BALANCE, BigDecimal.ONE, DemoOutcome.SUCCESS)))
                .hasMessageContaining("толық төленген");
    }

    @Test
    void earlyCancellationRefundsEverythingAndKeepsHistory() {
        Booking b = bookingService.create(form("10:00", "13:00", 2));
        paymentService.pay(b.getId(), payment(PaymentType.DEPOSIT, null, DemoOutcome.SUCCESS));

        bookingService.cancelByCustomer(b.getBookingNumber());

        Booking cancelled = bookingService.getById(b.getId());
        List<Payment> history = paymentService.history(b.getId());
        assertThat(cancelled.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(cancelled.getPaymentStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(cancelled.getAmountPaid()).isEqualByComparingTo("0");
        assertThat(history).extracting(Payment::getPaymentType).containsExactly(PaymentType.DEPOSIT, PaymentType.REFUND);
        assertThat(history.get(1).getAmount()).isEqualByComparingTo("10800");
    }

    @Test
    void statusTransitionsFollowRules() {
        Booking b = bookingService.create(form("10:00", "11:00", 2));

        assertThatThrownBy(() -> bookingService.changeStatusByAdmin(b.getId(), BookingStatus.COMPLETED))
                .hasMessageContaining("мүмкін емес");

        bookingService.changeStatusByAdmin(b.getId(), BookingStatus.CONFIRMED);
        // Әлі аяқталмаған брондауды "аяқталды" деп белгілеуге болмайды.
        assertThatThrownBy(() -> bookingService.changeStatusByAdmin(b.getId(), BookingStatus.COMPLETED))
                .hasMessageContaining("аяқталған жоқ");

        bookingService.changeStatusByAdmin(b.getId(), BookingStatus.CANCELLED);
        assertThatThrownBy(() -> bookingService.changeStatusByAdmin(b.getId(), BookingStatus.CONFIRMED))
                .hasMessageContaining("мүмкін емес");
        assertThatThrownBy(() -> paymentService.pay(b.getId(), payment(PaymentType.DEPOSIT, null, DemoOutcome.SUCCESS)))
                .hasMessageContaining("төлем қабылданбайды");
    }

    @Test
    void lookupRequiresMatchingEmail() {
        Booking b = bookingService.create(form("10:00", "11:00", 2));

        assertThat(bookingService.lookup(b.getBookingNumber().toLowerCase(), "ORGANIZER@example.kz")).isPresent();
        assertThat(bookingService.lookup(b.getBookingNumber(), "someone@example.kz")).isEmpty();
    }

    @Test
    void concurrentRequestsForSameSlotCreateOnlyOneBooking() throws Exception {
        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            Callable<Boolean> task = () -> {
                start.await();
                try {
                    bookingService.create(form("18:00", "20:00", 2));
                    return true;
                } catch (BookingConflictException e) {
                    return false;
                }
            };
            results.add(pool.submit(task));
        }
        start.countDown();
        int created = 0;
        for (Future<Boolean> f : results) {
            if (f.get()) {
                created++;
            }
        }
        pool.shutdown();

        assertThat(created).isEqualTo(1);
        assertThat(bookingRepository.countOverlapping(room.getId(), day.atTime(18, 0), day.atTime(20, 0),
                BookingStatus.BLOCKING)).isEqualTo(1);
    }

    private BookingForm form(String start, String end, int guests) {
        BookingForm f = new BookingForm();
        f.setRoomId(room.getId());
        f.setDate(day);
        f.setStartTime(LocalTime.parse(start));
        f.setEndTime(LocalTime.parse(end));
        f.setGuestCount(guests);
        f.setPurpose("Тест кездесуі");
        f.setFirstName("Тест");
        f.setLastName("Ұйымдастырушы");
        f.setPhone("+7 701 000 00 00");
        f.setEmail("organizer@example.kz");
        return f;
    }

    private static PaymentForm payment(PaymentType type, BigDecimal amount, DemoOutcome outcome) {
        PaymentForm p = new PaymentForm();
        p.setType(type);
        p.setAmount(amount);
        p.setOutcome(outcome);
        p.setAcknowledgeDemo(true);
        p.setIdempotencyKey(UUID.randomUUID().toString());
        return p;
    }
}
