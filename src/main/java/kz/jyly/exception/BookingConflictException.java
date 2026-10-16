package kz.jyly.exception;

/** Таңдалған уақыт аралығы басқа брондаумен қиылысады. */
public class BookingConflictException extends BusinessException {

    public BookingConflictException() {
        super("Бұл уақыт аралығы бос емес. Басқа уақытты немесе басқа бөлмені таңдап, қайта көріңіз.");
    }
}
