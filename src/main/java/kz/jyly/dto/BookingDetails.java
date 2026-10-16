package kz.jyly.dto;

import kz.jyly.entity.Booking;
import kz.jyly.entity.Guest;
import kz.jyly.entity.Payment;

import java.math.BigDecimal;
import java.util.List;

/**
 * Брондау беті үшін дайын деректер: шешімдер (не істеуге болады) сервисте есептеледі,
 * шаблонда тек көрсетіледі.
 */
public record BookingDetails(Booking booking,
                             List<Guest> guests,
                             List<Payment> payments,
                             boolean canPayDeposit,
                             boolean canPayBalance,
                             boolean canCancel,
                             boolean canEdit,
                             BigDecimal refundIfCancelledNow,
                             boolean freeCancellation) {
}
