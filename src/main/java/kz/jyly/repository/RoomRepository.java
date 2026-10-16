package kz.jyly.repository;

import jakarta.persistence.LockModeType;
import kz.jyly.entity.Room;
import kz.jyly.enums.RoomType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findBySlug(String slug);

    boolean existsBySlug(String slug);

    List<Room> findByActiveTrue(Sort sort);

    long countByActiveTrue();

    /**
     * Бөлме жолын жазуға құлыптайды (SELECT ... FOR UPDATE).
     * Бір бөлмеге параллель брондау сұраулары кезекпен орындалады.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);

    @Query("""
            select r from Room r
            where r.active = true
              and (:guests is null or r.capacity >= :guests)
              and (:maxPrice is null or r.hourlyRate <= :maxPrice)
              and (:type is null or r.type = :type)
            """)
    List<Room> search(@Param("guests") Integer guests,
                      @Param("maxPrice") BigDecimal maxPrice,
                      @Param("type") RoomType type,
                      Sort sort);
}
