package kz.jyly.repository;

import kz.jyly.entity.Guest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GuestRepository extends JpaRepository<Guest, Long> {

    List<Guest> findByBookingIdOrderByIdAsc(Long bookingId);
}
