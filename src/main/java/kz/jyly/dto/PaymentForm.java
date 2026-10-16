package kz.jyly.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.jyly.enums.DemoOutcome;
import kz.jyly.enums.PaymentType;

import java.math.BigDecimal;

/** Демо төлем формасы. Сома тек BALANCE үшін қолданылады, депозит сомасын сервер өзі есептейді. */
public class PaymentForm {

    @NotNull(message = "Төлем түрін таңдаңыз")
    private PaymentType type;

    private BigDecimal amount;

    @NotBlank(message = "Форма ескірген. Бетті жаңартыңыз")
    @Size(max = 64)
    private String idempotencyKey;

    @NotNull(message = "Демо нәтижесін таңдаңыз")
    private DemoOutcome outcome = DemoOutcome.SUCCESS;

    @AssertTrue(message = "Демо режимде нақты ақша есептен шығарылмайтынын растаңыз")
    private boolean acknowledgeDemo;

    public PaymentType getType() { return type; }
    public void setType(PaymentType type) { this.type = type; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public DemoOutcome getOutcome() { return outcome; }
    public void setOutcome(DemoOutcome outcome) { this.outcome = outcome; }
    public boolean isAcknowledgeDemo() { return acknowledgeDemo; }
    public void setAcknowledgeDemo(boolean acknowledgeDemo) { this.acknowledgeDemo = acknowledgeDemo; }
}
