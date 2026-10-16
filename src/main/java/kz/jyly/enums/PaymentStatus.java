package kz.jyly.enums;

/** Брондаудың жалпы төлем күйі (сәтті төлемдер негізінде есептеледі). */
public enum PaymentStatus {
    UNPAID("Төленбеген", "neutral"),
    DEPOSIT_PAID("Депозит төленді", "info"),
    PARTIALLY_PAID("Ішінара төленді", "warning"),
    PAID("Толық төленді", "success"),
    REFUNDED("Қайтарылды", "danger");

    private final String label;
    private final String tone;

    PaymentStatus(String label, String tone) {
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
