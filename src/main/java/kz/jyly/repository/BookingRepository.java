package kz.jyly.repository;

import jakarta.persistence.LockModeType;
import kz.jyly.entity.Booking;
import kz.jyly.enums.BookingStatus;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Optional<Booking> findByBookingNumber(String bookingNumber);

    boolean existsByBookingNumber(String bookingNumber);

    List<Booking> findByBookingNumberIn(Collection<String> numbers, Sort sort);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from Booking b where b.id = :id")
    Optional<Booking> findByIdForUpdate(@Param("id") Long id);

    /** Екі аралық қиылысады: existingStart < requestedEnd && existingEnd > requestedStart. */
    @Query("""
            select count(b) from Booking b
            where b.room.id = :roomId
              and b.status in :statuses
              and b.startDateTime < :end
              and b.endDateTime > :start
            """)
    long countOverlapping(@Param("roomId") Long roomId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            select distinct b.room.id from Booking b
            where b.status in :statuses
              and b.startDateTime < :end
              and b.endDateTime > :start
            """)
    List<Long> findBusyRoomIds(@Param("start") LocalDateTime start,
                               @Param("end") LocalDateTime end,
                               @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            select b from Booking b
            where b.room.id = :roomId
              and b.status in :statuses
              and b.startDateTime < :to
              and b.endDateTime > :from
            order by b.startDateTime
            """)
    List<Booking> findBlockingInRange(@Param("roomId") Long roomId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("statuses") Collection<BookingStatus> statuses);

    long countByStatus(BookingStatus status);

    List<Booking> findTop6ByOrderByCreatedAtDesc();

    List<Booking> findByCreatedAtGreaterThanEqual(LocalDateTime from);

    List<Booking> findByCustomerIdOrderByStartDateTimeDesc(Long customerId);

    @Query("select coalesce(sum(b.totalAmount - b.amountPaid), 0) from Booking b where b.status in :statuses")
    BigDecimal sumOutstanding(@Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            select b from Booking b join b.room r join b.customer c
            where (:q is null
                   or lower(b.bookingNumber) like :q
                   or lower(c.firstName) like :q
                   or lower(c.lastName) like :q
                   or lower(concat(c.firstName, ' ', c.lastName)) like :q
                   or lower(c.email) like :q
                   or lower(r.name) like :q)
              and (:status is null or b.status = :status)
              and (:from is null or b.startDateTime >= :from)
              and (:to is null or b.startDateTime < :to)
            order by b.startDateTime desc
            """)
    List<Booking> adminSearch(@Param("q") String q,
                              @Param("status") BookingStatus status,
                              @Param("from") LocalDateTime from,
                              @Param("to") LocalDateTime to);
}
