package kz.jyly.service;

import kz.jyly.dto.DashboardStats;
import kz.jyly.dto.DashboardStats.DailyStat;
import kz.jyly.entity.Booking;
import kz.jyly.entity.Payment;
import kz.jyly.enums.BookingStatus;
import kz.jyly.enums.TransactionStatus;
import kz.jyly.repository.BookingRepository;
import kz.jyly.repository.PaymentRepository;
import kz.jyly.repository.RoomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminStatsService {

    static final int CHART_DAYS = 14;

    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final BookingRules rules;

    public AdminStatsService(BookingRepository bookingRepository,
                             PaymentRepository paymentRepository,
                             RoomRepository roomRepository,
                             BookingRules rules) {
        this.bookingRepository = bookingRepository;
        this.paymentRepository = paymentRepository;
        this.roomRepository = roomRepository;
        this.rules = rules;
    }

    @Transactional(readOnly = true)
    public DashboardStats dashboard() {
        BigDecimal incoming = paymentRepository.sumAll(TransactionStatus.SUCCEEDED, PaymentService.INCOMING);
        BigDecimal refunds = paymentRepository.sumAll(TransactionStatus.SUCCEEDED, PaymentService.OUTGOING);

        LocalDate today = rules.today();
        LocalDate from = today.minusDays(CHART_DAYS - 1L);
        Map<LocalDate, Long> bookingsPerDay = new HashMap<>();
        for (Booking b : bookingRepository.findByCreatedAtGreaterThanEqual(from.atStartOfDay())) {
            bookingsPerDay.merge(b.getCreatedAt().toLocalDate(), 1L, Long::sum);
        }
        Map<LocalDate, BigDecimal> revenuePerDay = new HashMap<>();
        for (Payment p : paymentRepository.findByStatusAndCreatedAtGreaterThanEqual(TransactionStatus.SUCCEEDED, from.atStartOfDay())) {
            BigDecimal signed = p.getPaymentType().isIncoming() ? p.getAmount() : p.getAmount().negate();
            revenuePerDay.merge(p.getCreatedAt().toLocalDate(), signed, BigDecimal::add);
        }

        List<DailyStat> daily = new ArrayList<>();
        long maxBookings = 0;
        BigDecimal maxRevenue = BigDecimal.ZERO;
        for (LocalDate d = from; !d.isAfter(today); d = d.plusDays(1)) {
            long count = bookingsPerDay.getOrDefault(d, 0L);
            BigDecimal revenue = revenuePerDay.getOrDefault(d, BigDecimal.ZERO);
            daily.add(new DailyStat(d, count, revenue));
            maxBookings = Math.max(maxBookings, count);
            if (revenue.compareTo(maxRevenue) > 0) {
                maxRevenue = revenue;
            }
        }

        return new DashboardStats(
                bookingRepository.count(),
                bookingRepository.countByStatus(BookingStatus.PENDING),
                bookingRepository.countByStatus(BookingStatus.CONFIRMED),
                incoming.subtract(refunds),
                bookingRepository.sumOutstanding(BookingStatus.BILLABLE),
                roomRepository.countByActiveTrue(),
                bookingRepository.findTop6ByOrderByCreatedAtDesc(),
                daily,
                (int) Math.max(maxBookings, 1),
                maxRevenue.signum() > 0 ? maxRevenue : BigDecimal.ONE);
    }
}
