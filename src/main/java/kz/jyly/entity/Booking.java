package kz.jyly.entity;

import jakarta.persistence.*;
import kz.jyly.enums.BookingStatus;
import kz.jyly.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bookings",
        uniqueConstraints = @UniqueConstraint(name = "uk_booking_number", columnNames = "booking_number"),
        indexes = {
                @Index(name = "idx_booking_room_time", columnList = "room_id, start_date_time, end_date_time"),
                @Index(name = "idx_booking_status", columnList = "status"),
                @Index(name = "idx_booking_customer", columnList = "customer_id")
        })
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Кездейсоқ, болжауға қиын нөмір (мысалы JY-7KQ4-M2XP). */
    @Column(name = "booking_number", nullable = false, length = 20)
    private String bookingNumber;

    /** Бөлме немесе клиент жойылса да, тарих сақталуы үшін каскадты жою жоқ. */
    @ManyToOne(optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;

    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;

    @Column(nullable = false)
    private int guestCount;

    @Column(nullable = false, length = 120)
    private String purpose;

    @Column(length = 1000)
    private String notes;

    /** Брондау кезіндегі сағаттық баға: кейін бөлме бағасы өзгерсе де сома өзгермейді. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal hourlyRate;

    @Column(nullable = false)
    private int billableHours;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    /** Сәтті төлемдер минус қайтарулар. Payment кестесінен PaymentService қайта есептейді. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amountPaid = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus paymentStatus = PaymentStatus.UNPAID;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    @Column(length = 300)
    private String cancellationReason;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    private List<Guest> guests = new ArrayList<>();

    /** Төлемдер ешқашан каскадпен жойылмайды. */
    @OneToMany(mappedBy = "booking")
    @OrderBy("createdAt ASC")
    private List<Payment> payments = new ArrayList<>();

    @Version
    private long version;

    public void addGuest(Guest guest) {
        guest.setBooking(this);
        guests.add(guest);
    }

    public BigDecimal getRemainingAmount() {
        BigDecimal remaining = totalAmount.subtract(amountPaid);
        return remaining.signum() > 0 ? remaining : BigDecimal.ZERO;
    }

    public long getDurationMinutes() {
        return Duration.between(startDateTime, endDateTime).toMinutes();
    }

    public boolean isDepositCovered() {
        return amountPaid.compareTo(depositAmount) >= 0;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBookingNumber() { return bookingNumber; }
    public void setBookingNumber(String bookingNumber) { this.bookingNumber = bookingNumber; }
    public Room getRoom() { return room; }
    public void setRoom(Room room) { this.room = room; }
    public Customer getCustomer() { return customer; }
    public void setCustomer(Customer customer) { this.customer = customer; }
    public LocalDateTime getStartDateTime() { return startDateTime; }
    public void setStartDateTime(LocalDateTime startDateTime) { this.startDateTime = startDateTime; }
    public LocalDateTime getEndDateTime() { return endDateTime; }
    public void setEndDateTime(LocalDateTime endDateTime) { this.endDateTime = endDateTime; }
    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(BigDecimal hourlyRate) { this.hourlyRate = hourlyRate; }
    public int getBillableHours() { return billableHours; }
    public void setBillableHours(int billableHours) { this.billableHours = billableHours; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public BigDecimal getDepositAmount() { return depositAmount; }
    public void setDepositAmount(BigDecimal depositAmount) { this.depositAmount = depositAmount; }
    public BigDecimal getAmountPaid() { return amountPaid; }
    public void setAmountPaid(BigDecimal amountPaid) { this.amountPaid = amountPaid; }
    public BookingStatus getStatus() { return status; }
    public void setStatus(BookingStatus status) { this.status = status; }
    public PaymentStatus getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(PaymentStatus paymentStatus) { this.paymentStatus = paymentStatus; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(LocalDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public String getCancellationReason() { return cancellationReason; }
    public void setCancellationReason(String cancellationReason) { this.cancellationReason = cancellationReason; }
    public List<Guest> getGuests() { return guests; }
    public List<Payment> getPayments() { return payments; }
    public long getVersion() { return version; }
}
