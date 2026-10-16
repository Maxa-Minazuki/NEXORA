package kz.jyly.enums;

public enum PaymentType {
    DEPOSIT("Депозит"),
    BALANCE("Қалған төлем"),
    REFUND("Қайтару");

    private final String label;

    PaymentType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public boolean isIncoming() {
        return this != REFUND;
    }
}
