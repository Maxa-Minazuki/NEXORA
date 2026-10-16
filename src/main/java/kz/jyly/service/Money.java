package kz.jyly.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Ақша сомасын теңгемен көрсету: 36000 -> "36 000 ₸". */
public final class Money {

    private static final char NBSP = ' ';

    private Money() {
    }

    public static String format(BigDecimal amount) {
        if (amount == null) {
            return "—";
        }
        BigDecimal whole = amount.setScale(0, RoundingMode.HALF_UP);
        String digits = whole.abs().toPlainString();
        StringBuilder sb = new StringBuilder();
        int firstGroup = digits.length() % 3;
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (i - firstGroup) % 3 == 0) {
                sb.append(NBSP);
            }
            sb.append(digits.charAt(i));
        }
        return (whole.signum() < 0 ? "−" : "") + sb + NBSP + "₸";
    }
}
