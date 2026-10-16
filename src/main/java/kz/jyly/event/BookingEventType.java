package kz.jyly.event;

/** Брондаудың өмірлік цикліндегі бақыланатын оқиғалар. Тек жүйеде бар статус ауысулары. */
public enum BookingEventType {
    /** Жаңа брондау жасалды (PENDING). */
    BOOKING_CREATED("Брондау жасалды"),
    /** PENDING -> CONFIRMED: депозит төленді немесе әкімші растады. */
    BOOKING_CONFIRMED("Брондау расталды"),
    /** PENDING/CONFIRMED -> CANCELLED: клиент немесе әкімші тоқтатты. */
    BOOKING_CANCELLED("Брондау тоқтатылды");

    private final String label;

    BookingEventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
