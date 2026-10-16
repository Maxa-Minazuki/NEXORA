package kz.jyly.service;

import kz.jyly.dto.PaymentForm;
import kz.jyly.entity.Booking;
import kz.jyly.enums.BookingStatus;
import kz.jyly.enums.DemoOutcome;
import kz.jyly.enums.PaymentStatus;
import kz.jyly.enums.PaymentType;
import kz.jyly.exception.BusinessException;
import kz.jyly.repository.BookingRepository;
import kz.jyly.repository.PaymentRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceUnitTest {

    @Mock
    PaymentRepository paymentRepository;
    @Mock
    BookingRepository bookingRepository;
    @Mock
    BookingNumberGenerator generator;
    @Mock
    BookingRules rules;

    @InjectMocks
    PaymentService paymentService;

    @Test
    void paymentStatusIsDerivedFromSuccessfulAmounts() {
        BigDecimal total = new BigDecimal("36000");
        BigDecimal deposit = new BigDecimal("10800");
        BigDecimal zero = BigDecimal.ZERO;

        assertThat(PaymentService.resolveStatus(total, deposit, zero, zero)).isEqualTo(PaymentStatus.UNPAID);
        assertThat(PaymentService.resolveStatus(total, deposit, deposit, zero)).isEqualTo(PaymentStatus.DEPOSIT_PAID);
        assertThat(PaymentService.resolveStatus(total, deposit, new BigDecimal("20000"), zero)).isEqualTo(PaymentStatus.PARTIALLY_PAID);
        assertThat(PaymentService.resolveStatus(total, deposit, total, zero)).isEqualTo(PaymentStatus.PAID);
        assertThat(PaymentService.resolveStatus(total, deposit, zero, deposit)).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void lateCancellationKeepsDepositAndRefundsOnlyTheRest() {
        Booking b = booking("36000", "10800", "20000");

        assertThat(paymentService.refundAmount(b, true)).isEqualByComparingTo("20000");
        assertThat(paymentService.refundAmount(b, false)).isEqualByComparingTo("9200");

        Booking onlyDeposit = booking("36000", "10800", "10800");
        assertThat(paymentService.refundAmount(onlyDeposit, false)).isEqualByComparingTo("0");
    }

    @Test
    void cancelledBookingDoesNotAcceptPaymentsAndNothingIsSaved() {
        Booking b = booking("36000", "10800", "0");
        b.setId(7L);
        b.setStatus(BookingStatus.CANCELLED);
        when(paymentRepository.findByIdempotencyKey("k1")).thenReturn(Optional.empty());
        when(bookingRepository.findByIdForUpdate(7L)).thenReturn(Optional.of(b));

        PaymentForm form = new PaymentForm();
        form.setType(PaymentType.DEPOSIT);
        form.setIdempotencyKey("k1");
        form.setOutcome(DemoOutcome.SUCCESS);

        assertThatThrownBy(() -> paymentService.pay(7L, form))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("төлем қабылданбайды");
        verify(paymentRepository, never()).save(any());
    }

    private static Booking booking(String total, String deposit, String paid) {
        Booking b = new Booking();
        b.setTotalAmount(new BigDecimal(total));
        b.setDepositAmount(new BigDecimal(deposit));
        b.setAmountPaid(new BigDecimal(paid));
        return b;
    }
}
