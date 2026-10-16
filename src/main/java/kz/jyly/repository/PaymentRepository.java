package kz.jyly.repository;

import kz.jyly.entity.Payment;
import kz.jyly.enums.PaymentType;
import kz.jyly.enums.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingIdOrderByCreatedAtAscIdAsc(Long bookingId);

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    boolean existsByTransactionReference(String transactionReference);

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.booking.id = :bookingId and p.status = :status and p.paymentType in :types
            """)
    BigDecimal sumForBooking(@Param("bookingId") Long bookingId,
                             @Param("status") TransactionStatus status,
                             @Param("types") Collection<PaymentType> types);

    @Query("""
            select coalesce(sum(p.amount), 0) from Payment p
            where p.status = :status and p.paymentType in :types
            """)
    BigDecimal sumAll(@Param("status") TransactionStatus status,
                      @Param("types") Collection<PaymentType> types);

    List<Payment> findByStatusAndCreatedAtGreaterThanEqual(TransactionStatus status, LocalDateTime from);

    @Query("""
            select p from Payment p
            where (:status is null or p.status = :status)
              and (:type is null or p.paymentType = :type)
            order by p.createdAt desc, p.id desc
            """)
    List<Payment> filter(@Param("status") TransactionStatus status, @Param("type") PaymentType type);
}
