package kz.jyly.controller;

import kz.jyly.service.Money;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Шаблондарда қолданылатын қазақша форматтау: ${@fmt.money(booking.totalAmount)}.
 */
@Component("fmt")
public class Fmt {

    private static final String[] MONTHS = {"қаңтар", "ақпан", "наурыз", "сәуір", "мамыр", "маусым",
            "шілде", "тамыз", "қыркүйек", "қазан", "қараша", "желтоқсан"};
    private static final String[] WEEKDAYS = {"дүйсенбі", "сейсенбі", "сәрсенбі", "бейсенбі", "жұма", "сенбі", "жексенбі"};
    private static final String[] WEEKDAYS_SHORT = {"Дс", "Сс", "Ср", "Бс", "Жм", "Сб", "Жс"};
    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DD_MM = DateTimeFormatter.ofPattern("dd.MM");

    public String money(BigDecimal amount) {
        return Money.format(amount);
    }

    public String moneyOf(long amount) {
        return Money.format(BigDecimal.valueOf(amount));
    }

    /** ISO жолынан ("2026-10-14") қазақша толық күн. */
    public String dayLongIso(String isoDate) {
        return dayLong(LocalDate.parse(isoDate));
    }

    /** Input өрісі үшін: 36000.00 -> "36000". */
    public String plain(BigDecimal amount) {
        return amount == null ? "" : amount.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    public String date(TemporalAccessor value) {
        LocalDate d = toDate(value);
        return d == null ? "" : d.getDayOfMonth() + " " + MONTHS[d.getMonthValue() - 1] + " " + d.getYear();
    }

    /** "14 қазан, сейсенбі" */
    public String dayLong(TemporalAccessor value) {
        LocalDate d = toDate(value);
        return d == null ? "" : d.getDayOfMonth() + " " + MONTHS[d.getMonthValue() - 1] + ", "
                + WEEKDAYS[d.getDayOfWeek().getValue() - 1];
    }

    public String dayMonth(TemporalAccessor value) {
        LocalDate d = toDate(value);
        return d == null ? "" : d.getDayOfMonth() + " " + MONTHS[d.getMonthValue() - 1];
    }

    public String weekdayShort(TemporalAccessor value) {
        LocalDate d = toDate(value);
        return d == null ? "" : WEEKDAYS_SHORT[d.getDayOfWeek().getValue() - 1];
    }

    public String shortDate(TemporalAccessor value) {
        LocalDate d = toDate(value);
        return d == null ? "" : d.format(DD_MM);
    }

    public String time(TemporalAccessor value) {
        if (value instanceof LocalDateTime dt) {
            return dt.format(HH_MM);
        }
        if (value instanceof LocalTime t) {
            return t.format(HH_MM);
        }
        return "";
    }

    public String dateTime(LocalDateTime value) {
        return value == null ? "" : date(value) + ", " + value.format(HH_MM);
    }

    /** "14 қазан 2026 · 10:00–13:00" */
    public String range(LocalDateTime start, LocalDateTime end) {
        return date(start) + " · " + time(start) + "–" + time(end);
    }

    public String duration(long minutes) {
        long h = minutes / 60;
        long m = minutes % 60;
        if (h == 0) {
            return m + " мин";
        }
        return m == 0 ? h + " сағ" : h + " сағ " + m + " мин";
    }

    public String initials(String first, String last) {
        String a = first == null || first.isBlank() ? "" : first.substring(0, 1);
        String b = last == null || last.isBlank() ? "" : last.substring(0, 1);
        return (a + b).toUpperCase();
    }

    /** Диаграмма бағанының биіктігі, пайызбен (кемінде 2%, бос болмауы үшін). */
    public int percent(BigDecimal part, BigDecimal whole) {
        if (part == null || whole == null || whole.signum() <= 0 || part.signum() <= 0) {
            return 0;
        }
        int p = part.multiply(BigDecimal.valueOf(100)).divide(whole, 0, RoundingMode.HALF_UP).intValue();
        return Math.max(2, Math.min(100, p));
    }

    public int percentOfCount(long part, long whole) {
        if (whole <= 0 || part <= 0) {
            return 0;
        }
        return (int) Math.max(2, Math.min(100, Math.round(part * 100.0 / whole)));
    }

    private static LocalDate toDate(TemporalAccessor value) {
        if (value instanceof LocalDateTime dt) {
            return dt.toLocalDate();
        }
        if (value instanceof LocalDate d) {
            return d;
        }
        return null;
    }
}
