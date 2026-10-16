package kz.jyly.service;

import kz.jyly.config.JylyProperties;
import kz.jyly.dto.PriceQuote;
import kz.jyly.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Брондау құнын есептейді. Барлық ақша мәндері BigDecimal арқылы, бүтін теңгеге дейін
 * HALF_UP ережесімен дөңгелектенеді.
 *
 * <p>Клуб ережесі: <b>әр басталған сағат толық сағат ретінде төленеді</b>.
 * Мысалы, 1 сағат 30 минут = 2 төленетін сағат.</p>
 *
 * <pre>
 * totalAmount     = hourlyRate × billableHours
 * depositAmount   = totalAmount × depositPercent / 100
 * remainingAmount = totalAmount − amountPaid
 * </pre>
 */
@Service
public class PricingService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final BigDecimal depositPercent;

    public PricingService(JylyProperties properties) {
        BigDecimal percent = properties.getDepositPercent();
        if (percent == null || percent.signum() < 0 || percent.compareTo(HUNDRED) > 0) {
            throw new IllegalStateException("jyly.deposit-percent must be between 0 and 100");
        }
        this.depositPercent = percent;
    }

    public PriceQuote quote(BigDecimal hourlyRate, LocalDateTime start, LocalDateTime end) {
        if (hourlyRate == null || hourlyRate.signum() <= 0) {
            throw new IllegalArgumentException("Hourly rate must be positive");
        }
        if (start == null || end == null || !end.isAfter(start)) {
            throw new BusinessException("Аяқталу уақыты басталу уақытынан кейін болуы керек", "endTime");
        }
        long minutes = Duration.between(start, end).toMinutes();
        int hours = billableHours(minutes);
        BigDecimal rate = toTenge(hourlyRate);
        BigDecimal total = toTenge(rate.multiply(BigDecimal.valueOf(hours)));
        BigDecimal deposit = depositFor(total);
        return new PriceQuote(rate, minutes, hours, total, depositPercent, deposit, total.subtract(deposit));
    }

    /** Әр басталған сағат толық есептеледі: 60 мин -> 1, 61 мин -> 2, 90 мин -> 2. */
    public int billableHours(long durationMinutes) {
        if (durationMinutes <= 0) {
            throw new BusinessException("Брондау ұзақтығы нөлден көп болуы керек", "endTime");
        }
        return (int) ((durationMinutes + 59) / 60);
    }

    public BigDecimal depositFor(BigDecimal totalAmount) {
        return totalAmount.multiply(depositPercent).divide(HUNDRED, 0, RoundingMode.HALF_UP);
    }

    /** Қалған төлем ешқашан теріс болмайды. */
    public BigDecimal remaining(BigDecimal totalAmount, BigDecimal amountPaid) {
        BigDecimal remaining = totalAmount.subtract(amountPaid);
        return remaining.signum() > 0 ? remaining : BigDecimal.ZERO;
    }

    public BigDecimal getDepositPercent() {
        return depositPercent;
    }

    private static BigDecimal toTenge(BigDecimal value) {
        return value.setScale(0, RoundingMode.HALF_UP);
    }
}
