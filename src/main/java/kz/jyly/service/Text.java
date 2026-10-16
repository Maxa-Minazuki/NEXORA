package kz.jyly.service;

import java.util.Locale;

/** Мәтінмен жұмыс істейтін шағын көмекші әдістер. */
public final class Text {

    private Text() {
    }

    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** "  Айгерім " -> "%айгерім%" (LIKE үшін); бос мән -> null. % және _ таңбалары алынып тасталады. */
    public static String likePattern(String query) {
        String q = blankToNull(query);
        if (q == null) {
            return null;
        }
        return "%" + q.toLowerCase(Locale.ROOT).replace("%", "").replace("_", "") + "%";
    }
}
