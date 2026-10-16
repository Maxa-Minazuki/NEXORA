package kz.jyly.service;

import kz.jyly.config.JylyProperties;
import kz.jyly.entity.Booking;
import kz.jyly.entity.Room;
import kz.jyly.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Уақыт бекітілген: "қазір" = 2030-05-10 12:00 (Алматы). */
class BookingRulesTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Almaty");
    private static final LocalDateTime NOW = LocalDateTime.of(2030, 5, 10, 12, 0);

    private BookingRules rules;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(NOW.atZone(ZONE).toInstant(), ZONE);
        rules = new BookingRules(clock, new JylyProperties());
    }

    @Test
    void validFutureIntervalPasses() {
        assertThatCode(() -> rules.validateInterval(NOW.plusDays(1).withHour(10), NOW.plusDays(1).withHour(13)))
                .doesNotThrowAnyException();
    }

    @Test
    void pastDateIsRejected() {
        assertThatThrownBy(() -> rules.validateInterval(NOW.minusDays(1).withHour(10), NOW.minusDays(1).withHour(12)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Өткен күнге");
    }

    @Test
    void earlierTimeTodayIsRejected() {
        assertThatThrownBy(() -> rules.validateInterval(NOW.withHour(9), NOW.withHour(11)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("өтіп кеткен");
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThatThrownBy(() -> rules.validateInterval(NOW.plusDays(1).withHour(14), NOW.plusDays(1).withHour(12)))
                .isInstanceOf(BusinessException.class)
                .extracting("field").isEqualTo("endTime");
    }

    @Test
    void tooFarInFutureIsRejected() {
        LocalDateTime start = NOW.plusDays(91).withHour(10);
        assertThatThrownBy(() -> rules.validateInterval(start, start.plusHours(1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("90 күн");
    }

    @Test
    void outsideOpeningHoursIsRejected() {
        LocalDateTime day = NOW.plusDays(1);
        assertThatThrownBy(() -> rules.validateInterval(day.withHour(7), day.withHour(9)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("жұмыс істейді");
        assertThatThrownBy(() -> rules.validateInterval(day.withHour(21), day.withHour(23)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void offGridAndTooShortIntervalsAreRejected() {
        LocalDateTime day = NOW.plusDays(1).withHour(10);
        assertThatThrownBy(() -> rules.validateInterval(day.withMinute(15), day.withHour(12)))
                .hasMessageContaining("минуттық қадаммен");
        assertThatThrownBy(() -> rules.validateInterval(day, day.withMinute(30)))
                .hasMessageContaining("Ең қысқа");
    }

    @Test
    void guestCountMustBePositiveAndWithinCapacity() {
        Room room = new Room();
        room.setCapacity(4);

        assertThatCode(() -> rules.validateGuests(4, room)).doesNotThrowAnyException();
        assertThatThrownBy(() -> rules.validateGuests(0, room)).hasMessageContaining("нөлден көп");
        assertThatThrownBy(() -> rules.validateGuests(5, room))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("сыйымдылығынан (4 адам)");
    }

    @Test
    void freeCancellationDependsOnHoursBeforeStart() {
        Booking soon = new Booking();
        soon.setStartDateTime(NOW.plusHours(5));
        Booking later = new Booking();
        later.setStartDateTime(NOW.plusHours(30));

        assertThat(rules.isFreeCancellation(soon)).isFalse();
        assertThat(rules.isFreeCancellation(later)).isTrue();
    }

    @Test
    void timeSlotsCoverOpeningHoursInHalfHourSteps() {
        assertThat(rules.timeSlots()).hasSize(29);
        assertThat(rules.timeSlots().get(0)).hasToString("08:00");
        assertThat(rules.timeSlots().get(28)).hasToString("22:00");
    }
}
