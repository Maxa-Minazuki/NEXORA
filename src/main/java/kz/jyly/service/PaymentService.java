package kz.jyly.service;

import kz.jyly.dto.PaymentForm;
import kz.jyly.entity.Booking;
import kz.jyly.entity.Payment;
import kz.jyly.enums.BookingStatus;
import kz.jyly.enums.DemoOutcome;
import kz.jyly.enums.PaymentStatus;
import kz.jyly.enums.PaymentType;
import kz.jyly.enums.TransactionStatus;
import kz.jyly.event.BookingEvent;
import kz.jyly.event.BookingEventType;
import kz.jyly.exception.BusinessException;
import kz.jyly.exception.NotFoundException;
import kz.jyly.repository.BookingRepository;
import kz.jyly.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ДЕМО төлем жүйесі. Нақты банк эквайрингі қосылмаған: ешқандай ақша есептен шығарылмайды.
 * Әр операция payments кестесіне жазылады; тек SUCCEEDED операциялар төленген сомаға кіреді.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    static final Set<PaymentType> INCOMING = EnumSet.of(PaymentType.DEPOSIT, PaymentType.BALANCE);
    static final Set<PaymentType> OUTGOING = EnumSet.of(PaymentType.REFUND);

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingNumberGenerator generator;
    private final BookingRules rules;
    private final BookingEventPublisher eventPublisher;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingRepository bookingRepository,
                          BookingNumberGenerator generator,
                          BookingRules rules,
                          BookingEventPublisher eventPublisher) {
        this.paymentRepository = paymentRepository;
        this.bookingRepository = bookingRepository;
        this.generator = generator;
        this.rules = rules;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Демо төлемді тіркейді. Брондау жолы құлыпталады, сондықтан бір брондауға параллель
     * төлемдер кезекпен өтеді және артық төлем мүмкін емес.
     */
    @Transactional
    public Payment pay(Long bookingId, PaymentForm form) {
        Optional<Payment> duplicate = findDuplicate(bookingId, form.getIdempotencyKey());
        if (duplicate.isPresent()) {
            return duplicate.get();
        }

        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new NotFoundException("Брондау табылмады"));

        // Құлыпты күтіп тұрған екінші сұрау бірінші сұрау сақтаған төлемді осы жерде көреді.
        duplicate = findDuplicate(bookingId, form.getIdempotencyKey());
        if (duplicate.isPresent()) {
            return duplicate.get();
        }

        if (!booking.getStatus().isActive()) {
            throw new BusinessException("Бұл брондау үшін төлем қабылданбайды");
        }
        BigDecimal remaining = booking.getRemainingAmount();
        if (remaining.signum() <= 0) {
            throw new BusinessException("Брондау толық төленген");
        }

        BigDecimal amount = switch (form.getType()) {
            case DEPOSIT -> depositAmountDue(booking);
            case BALANCE -> validateBalanceAmount(booking, form.getAmount(), remaining);
            case REFUND -> throw new BusinessException("Қайтару тек брондауды тоқтату арқылы жасалады");
        };

        boolean success = form.getOutcome() == DemoOutcome.SUCCESS;
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(amount);
        payment.setPaymentType(form.getType());
        payment.setStatus(success ? TransactionStatus.SUCCEEDED : TransactionStatus.FAILED);
        payment.setTransactionReference(generator.transactionReference("DEMO"));
        payment.setIdempotencyKey(form.getIdempotencyKey());
        payment.setDemo(true);
        payment.setNote(success ? "Демо режим: нақты ақша есептен шығарылған жоқ"
                : "Демо режим: банк операцияны қабылдамады (сынақ сценарийі)");
        payment.setCreatedAt(rules.timestamp());
        paymentRepository.save(payment);

        if (success) {
            recalculate(booking);
            if (booking.getStatus() == BookingStatus.PENDING && booking.isDepositCovered()) {
                booking.setStatus(BookingStatus.CONFIRMED);
                eventPublisher.publish(BookingEvent.of(BookingEventType.BOOKING_CONFIRMED, booking, rules.timestamp()));
            }
        }
        log.info("Demo payment {} for booking {}: type={}, amount={}, status={}",
                payment.getTransactionReference(), booking.getBookingNumber(),
                payment.getPaymentType(), amount, payment.getStatus());
        return payment;
    }

    /** Брондауды тоқтатқанда қайтарылатын сома (шақыратын сервис транзакция ашқан болуы керек). */
    @Transactional(propagation = Propagation.MANDATORY)
    public Optional<Payment> refundOnCancellation(Booking booking, boolean fullRefund) {
        BigDecimal refund = refundAmount(booking, fullRefund);
        if (refund.signum() <= 0) {
            return Optional.empty();
        }
        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setAmount(refund);
        payment.setPaymentType(PaymentType.REFUND);
        payment.setStatus(TransactionStatus.SUCCEEDED);
        payment.setTransactionReference(generator.transactionReference("DEMO-RF"));
        payment.setDemo(true);
        payment.setNote(fullRefund ? "Демо қайтару: толық сома (тегін тоқтату мерзімінде)"
                : "Демо қайтару: депозит ұсталды (кеш тоқтату)");
        payment.setCreatedAt(rules.timestamp());
        paymentRepository.save(payment);
        recalculate(booking);
        log.info("Demo refund {} for booking {}: amount={}", payment.getTransactionReference(),
                booking.getBookingNumber(), refund);
        return Optional.of(payment);
    }

    /**
     * Қайтару ережесі (демо):
     * тегін тоқтату мерзімінде — бүкіл төленген сома; кеш болса — депозиттен артық төленген бөлігі ғана.
     */
    public BigDecimal refundAmount(Booking booking, boolean fullRefund) {
        BigDecimal paid = booking.getAmountPaid();
        if (paid.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        if (fullRefund) {
            return paid;
        }
        BigDecimal refundable = paid.subtract(booking.getDepositAmount());
        return refundable.signum() > 0 ? refundable : BigDecimal.ZERO;
    }

    /** Төленген соманы payments кестесінен қайта есептейді (жалғыз шындық көзі). */
    public void recalculate(Booking booking) {
        BigDecimal incoming = paymentRepository.sumForBooking(booking.getId(), TransactionStatus.SUCCEEDED, INCOMING);
        BigDecimal refunded = paymentRepository.sumForBooking(booking.getId(), TransactionStatus.SUCCEEDED, OUTGOING);
        BigDecimal net = incoming.subtract(refunded);
        booking.setAmountPaid(net);
        booking.setPaymentStatus(resolveStatus(booking.getTotalAmount(), booking.getDepositAmount(), net, refunded));
    }

    public static PaymentStatus resolveStatus(BigDecimal total, BigDecimal deposit, BigDecimal netPaid, BigDecimal refunded) {
        if (refunded.signum() > 0) {
            return PaymentStatus.REFUNDED;
        }
        if (netPaid.signum() <= 0) {
            return PaymentStatus.UNPAID;
        }
        if (netPaid.compareTo(total) >= 0) {
            return PaymentStatus.PAID;
        }
        if (netPaid.compareTo(deposit) == 0) {
            return PaymentStatus.DEPOSIT_PAID;
        }
        return PaymentStatus.PARTIALLY_PAID;
    }

    @Transactional(readOnly = true)
    public List<Payment> history(Long bookingId) {
        return paymentRepository.findByBookingIdOrderByCreatedAtAscIdAsc(bookingId);
    }

    @Transactional(readOnly = true)
    public List<Payment> filter(TransactionStatus status, PaymentType type) {
        return paymentRepository.filter(status, type);
    }

    public BigDecimal depositAmountDue(Booking booking) {
        if (booking.isDepositCovered()) {
            throw new BusinessException("Депозит бұрын төленген");
        }
        return booking.getDepositAmount().subtract(booking.getAmountPaid());
    }

    private BigDecimal validateBalanceAmount(Booking booking, BigDecimal amount, BigDecimal remaining) {
        if (!booking.isDepositCovered()) {
            throw new BusinessException("Алдымен депозитті төлеңіз");
        }
        if (amount == null) {
            throw new BusinessException("Төлем сомасын енгізіңіз", "amount");
        }
        if (amount.signum() <= 0) {
            throw new BusinessException("Төлем сомасы нөлден көп болуы керек", "amount");
        }
        if (amount.stripTrailingZeros().scale() > 0) {
            throw new BusinessException("Соманы бүтін теңгемен енгізіңіз", "amount");
        }
        if (amount.compareTo(remaining) > 0) {
            throw new BusinessException("Төлем сомасы қалған төлемнен (" + Money.format(remaining) + ") аспауы керек", "amount");
        }
        return amount;
    }

    private Optional<Payment> findDuplicate(Long bookingId, String key) {
        if (key == null || key.isBlank()) {
            throw new BusinessException("Форма ескірген. Бетті жаңартып, қайта көріңіз");
        }
        Optional<Payment> existing = paymentRepository.findByIdempotencyKey(key);
        if (existing.isPresent() && !existing.get().getBooking().getId().equals(bookingId)) {
            throw new BusinessException("Төлем сұрауы жарамсыз. Бетті жаңартып, қайта көріңіз");
        }
        return existing;
    }
}
