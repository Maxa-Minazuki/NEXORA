package kz.jyly.exception;

/**
 * Бизнес-ереже бұзылғанда лақтырылады. Хабарлама пайдаланушыға тікелей көрсетіледі,
 * сондықтан ол әрқашан қазақ тілінде жазылады.
 */
public class BusinessException extends RuntimeException {

    /** Қате қай форма өрісіне қатысты (міндетті емес). */
    private final String field;

    public BusinessException(String message) {
        this(message, null);
    }

    public BusinessException(String message, String field) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
