package kz.jyly.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Болжауға қиын брондау нөмірі: JY-XXXX-XXXX (32^8 ≈ 10^12 нұсқа).
 * Шатастыратын таңбалар (0/O, 1/I) алфавитте жоқ.
 */
@Component
public class BookingNumberGenerator {

    private static final String ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random = new SecureRandom();

    public String next() {
        return "JY-" + block() + "-" + block();
    }

    public String transactionReference(String prefix) {
        return prefix + "-" + block() + block() + block();
    }

    private String block() {
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return sb.toString();
    }
}
