package kz.jyly.enums;

/** Жеке төлем операциясының нәтижесі. Тек SUCCEEDED ғана төленген сомаға қосылады. */
public enum TransactionStatus {
    SUCCEEDED("Сәтті", "success"),
    FAILED("Сәтсіз", "danger"),
    CANCELLED("Болдырылмады", "neutral");

    private final String label;
    private final String tone;

    TransactionStatus(String label, String tone) {
        this.label = label;
        this.tone = tone;
    }

    public String getLabel() {
        return label;
    }

    public String getTone() {
        return tone;
    }
}
