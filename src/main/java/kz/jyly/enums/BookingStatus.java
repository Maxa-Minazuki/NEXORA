package kz.jyly.enums;

import java.util.EnumSet;
import java.util.Set;

/**
 * Брондаудың өмірлік циклі.
 * PENDING -> CONFIRMED -> COMPLETED, ал PENDING/CONFIRMED -> CANCELLED.
 */
public enum BookingStatus {
    PENDING("Күтілуде", "warning"),
    CONFIRMED("Расталған", "success"),
    CANCELLED("Тоқтатылған", "danger"),
    COMPLETED("Аяқталған", "neutral");

    /** Бөлмені бос емес етіп ұстайтын статустар. Тоқтатылған брондау бөлмені бұғаттамайды. */
    public static final Set<BookingStatus> BLOCKING = EnumSet.of(PENDING, CONFIRMED, COMPLETED);

    /** Төлемі әлі күтілуі мүмкін статустар (қалған қарызды есептеу үшін). */
    public static final Set<BookingStatus> BILLABLE = EnumSet.of(PENDING, CONFIRMED, COMPLETED);

    private final String label;
    private final String tone;

    BookingStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() {
        return label;
    }

    public String getTone() {
        return tone;
    }

    public boolean isActive() {
        return this == PENDING || this == CONFIRMED;
    }

    public boolean canTransitionTo(BookingStatus target) {
        return switch (this) {
            case PENDING -> target == CONFIRMED || target == CANCELLED;
            case CONFIRMED -> target == COMPLETED || target == CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };
    }
}
