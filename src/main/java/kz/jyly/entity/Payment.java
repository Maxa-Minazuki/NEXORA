package kz.jyly.entity;

import jakarta.persistence.*;
import kz.jyly.enums.PaymentType;
import kz.jyly.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Бір төлем операциясы. Жазбалар өзгертілмейді және жойылмайды — бұл қаржы тарихы.
 * Барлық операциялар демо режимде жасалады: нақты ақша есептен шығарылмайды.
 */
@Entity
@Table(name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_payment_reference", columnNames = "transaction_reference"),
                @UniqueConstraint(name = "uk_payment_idempotency", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_payment_booking", columnList = "booking_id"),
                @Index(name = "idx_payment_created", columnList = "created_at")
        })
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TransactionStatus status;

    @Column(name = "transaction_reference", nullable = false, length = 40)
    private String transactionReference;

    /** Бір форманы екі рет жіберуден қорғайды: бір кілт — бір операция. */
    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(nullable = false)
    private boolean demo = true;

    @Column(length = 200)
    private String note;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public boolean isSucceeded() {
        return status == TransactionStatus.SUCCEEDED;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Booking getBooking() { return booking; }
    public void setBooking(Booking booking) { this.booking = booking; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public PaymentType getPaymentType() { return paymentType; }
    public void setPaymentType(PaymentType paymentType) { this.paymentType = paymentType; }
    public TransactionStatus getStatus() { return status; }
    public void setStatus(TransactionStatus status) { this.status = status; }
    public String getTransactionReference() { return transactionReference; }
    public void setTransactionReference(String transactionReference) { this.transactionReference = transactionReference; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public boolean isDemo() { return demo; }
    public void setDemo(boolean demo) { this.demo = demo; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
