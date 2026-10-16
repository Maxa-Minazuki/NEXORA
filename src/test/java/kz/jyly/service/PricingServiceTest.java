package kz.jyly.service;

import kz.jyly.config.JylyProperties;
import kz.jyly.dto.PriceQuote;
import kz.jyly.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PricingServiceTest {

    private static final LocalDateTime START = LocalDateTime.of(2030, 5, 10, 10, 0);

    private PricingService pricing;

    @BeforeEach
    void setUp() {
        JylyProperties properties = new JylyProperties();
        properties.setDepositPercent(new BigDecimal("30"));
        pricing = new PricingService(properties);
    }

    @Test
    void totalIsHourlyRateTimesHours() {
        PriceQuote q = pricing.quote(new BigDecimal("12000"), START, START.plusHours(3));

        assertThat(q.billableHours()).isEqualTo(3);
        assertThat(q.totalAmount()).isEqualByComparingTo("36000");
    }

    @Test
    void depositIsThirtyPercentOfTotal() {
        PriceQuote q = pricing.quote(new BigDecimal("12000"), START, START.plusHours(3));

        assertThat(q.depositAmount()).isEqualByComparingTo("10800");
        assertThat(q.balanceAfterDeposit()).isEqualByComparingTo("25200");
    }

    @Test
    void remainingIsTotalMinusPaidAndNeverNegative() {
        assertThat(pricing.remaining(new BigDecimal("36000"), new BigDecimal("10800"))).isEqualByComparingTo("25200");
        assertThat(pricing.remaining(new BigDecimal("36000"), new BigDecimal("36000"))).isEqualByComparingTo("0");
        assertThat(pricing.remaining(new BigDecimal("36000"), new BigDecimal("40000"))).isEqualByComparingTo("0");
    }

    @Test
    void everyStartedHourIsBilledAsFullHour() {
        assertThat(pricing.billableHours(60)).isEqualTo(1);
        assertThat(pricing.billableHours(61)).isEqualTo(2);
        assertThat(pricing.billableHours(90)).isEqualTo(2);
        assertThat(pricing.billableHours(120)).isEqualTo(2);

        PriceQuote q = pricing.quote(new BigDecimal("6000"), START, START.plusMinutes(90));
        assertThat(q.durationMinutes()).isEqualTo(90);
        assertThat(q.totalAmount()).isEqualByComparingTo("12000");
    }

    @Test
    void depositIsRoundedHalfUpToWholeTenge() {
        // 12 345 × 30% = 3 703,5 -> 3 704
        assertThat(pricing.depositFor(new BigDecimal("12345"))).isEqualByComparingTo("3704");
        // 12 343 × 30% = 3 702,9 -> 3 703
        assertThat(pricing.depositFor(new BigDecimal("12343"))).isEqualByComparingTo("3703");
        // 10 001 × 30% = 3 000,3 -> 3 000
        assertThat(pricing.depositFor(new BigDecimal("10001"))).isEqualByComparingTo("3000");
    }

    @Test
    void fractionalHourlyRateIsRoundedToWholeTenge() {
        PriceQuote q = pricing.quote(new BigDecimal("9999.50"), START, START.plusHours(1));
        assertThat(q.hourlyRate()).isEqualByComparingTo("10000");
        assertThat(q.totalAmount()).isEqualByComparingTo("10000");
        assertThat(q.depositAmount().scale()).isZero();
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThatThrownBy(() -> pricing.quote(new BigDecimal("12000"), START, START.minusHours(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Аяқталу уақыты");
    }

    @Test
    void zeroDurationIsRejected() {
        assertThatThrownBy(() -> pricing.quote(new BigDecimal("12000"), START, START))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> pricing.billableHours(0)).isInstanceOf(BusinessException.class);
    }

    @Test
    void depositPercentComesFromConfiguration() {
        JylyProperties properties = new JylyProperties();
        properties.setDepositPercent(new BigDecimal("50"));
        PricingService half = new PricingService(properties);

        assertThat(half.quote(new BigDecimal("10000"), START, START.plusHours(2)).depositAmount())
                .isEqualByComparingTo("10000");
    }

    @Test
    void invalidDepositPercentFailsFast() {
        JylyProperties properties = new JylyProperties();
        properties.setDepositPercent(new BigDecimal("120"));
        assertThatThrownBy(() -> new PricingService(properties)).isInstanceOf(IllegalStateException.class);
    }
}
