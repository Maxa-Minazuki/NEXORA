package kz.jyly.service;

import kz.jyly.config.JylyProperties;
import kz.jyly.entity.Booking;
import kz.jyly.entity.Room;
import kz.jyly.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/** Клубтың брондау ережелері: жұмыс уақыты, уақыт қадамы, ұзақтық, сыйымдылық. */
@Component
public class BookingRules {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final Clock clock;
    private final JylyProperties properties;

    public BookingRules(Clock clock, JylyProperties properties) {
        this.clock = clock;
        this.properties = properties;
    }

    public LocalDateTime now() {
        return LocalDateTime.now(clock).withSecond(0).withNano(0);
    }

    /** Жазбаларға арналған уақыт белгісі (секундпен). */
    public LocalDateTime timestamp() {
        return LocalDateTime.now(clock).withNano(0);
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public void validateInterval(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            throw new BusinessException("Күнді және уақытты толық таңдаңыз", "date");
        }
        if (!end.isAfter(start)) {
            throw new BusinessException("Аяқталу уақыты басталу уақытынан кейін болуы керек", "endTime");
        }
        if (!start.toLocalDate().equals(end.toLocalDate())) {
            throw new BusinessException("Брондау бір күн ішінде болуы керек", "endTime");
        }
        LocalDateTime now = now();
        if (start.toLocalDate().isBefore(now.toLocalDate())) {
            throw new BusinessException("Өткен күнге брондау жасау мүмкін емес", "date");
        }
        if (!start.isAfter(now)) {
            throw new BusinessException("Таңдалған басталу уақыты өтіп кеткен. Кейінірек уақытты таңдаңыз", "startTime");
        }
        if (start.toLocalDate().isAfter(now.toLocalDate().plusDays(properties.getMaxDaysAhead()))) {
            throw new BusinessException("Брондауды ең көбі " + properties.getMaxDaysAhead()
                    + " күн алдын ала жасауға болады", "date");
        }
        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();
        if (startTime.isBefore(properties.getOpeningTime()) || endTime.isAfter(properties.getClosingTime())) {
            throw new BusinessException("Клуб " + properties.getOpeningTime().format(HH_MM) + "–"
                    + properties.getClosingTime().format(HH_MM) + " аралығында жұмыс істейді", "startTime");
        }
        if (!isOnGrid(startTime) || !isOnGrid(endTime)) {
            throw new BusinessException("Уақытты " + properties.getSlotMinutes() + " минуттық қадаммен таңдаңыз", "startTime");
        }
        long minutes = Duration.between(start, end).toMinutes();
        if (minutes < properties.getMinDurationMinutes()) {
            throw new BusinessException("Ең қысқа брондау — " + properties.getMinDurationMinutes() + " минут", "endTime");
        }
        if (minutes > properties.getMaxDurationHours() * 60L) {
            throw new BusinessException("Ең ұзақ брондау — " + properties.getMaxDurationHours() + " сағат", "endTime");
        }
    }

    public void validateGuests(Integer guestCount, Room room) {
        if (guestCount == null || guestCount < 1) {
            throw new BusinessException("Қонақтар саны нөлден көп болуы керек", "guestCount");
        }
        if (guestCount > room.getCapacity()) {
            throw new BusinessException("Қонақтар саны бөлме сыйымдылығынан (" + room.getCapacity()
                    + " адам) аспауы керек", "guestCount");
        }
    }

    /**
     * Тегін тоқтату: басталуға дейін кемінде freeCancellationHours сағат қалса,
     * төленген соманың бәрі (депозитпен бірге) қайтарылады. Әйтпесе депозит ұсталып қалады.
     */
    public boolean isFreeCancellation(Booking booking) {
        return !now().plusHours(properties.getFreeCancellationHours()).isAfter(booking.getStartDateTime());
    }

    /** Формадағы уақыт таңдау тізімі: 08:00, 08:30, ..., 22:00. */
    public List<LocalTime> timeSlots() {
        List<LocalTime> slots = new ArrayList<>();
        LocalTime t = properties.getOpeningTime();
        while (!t.isAfter(properties.getClosingTime())) {
            slots.add(t);
            LocalTime next = t.plusMinutes(properties.getSlotMinutes());
            if (next.isBefore(t)) {
                break; // түн ортасынан өтіп кетпеу үшін
            }
            t = next;
        }
        return slots;
    }

    private boolean isOnGrid(LocalTime time) {
        return time.getSecond() == 0 && (time.toSecondOfDay() / 60) % properties.getSlotMinutes() == 0;
    }

    public JylyProperties getProperties() {
        return properties;
    }
}
