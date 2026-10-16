package kz.jyly.dto;

import java.math.BigDecimal;

/**
 * PricingService есептеген баға.
 *
 * @param hourlyRate      сағаттық баға
 * @param durationMinutes нақты ұзақтық, минут
 * @param billableHours   төленетін сағаттар (әр басталған сағат толық есептеледі)
 * @param totalAmount     жалпы құны
 * @param depositPercent  депозит пайызы
 * @param depositAmount   депозит мөлшері
 * @param balanceAfterDeposit депозиттен кейінгі қалған төлем
 */
public record PriceQuote(BigDecimal hourlyRate,
                         long durationMinutes,
                         int billableHours,
                         BigDecimal totalAmount,
                         BigDecimal depositPercent,
                         BigDecimal depositAmount,
                         BigDecimal balanceAfterDeposit) {
}
