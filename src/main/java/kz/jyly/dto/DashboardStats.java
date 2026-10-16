package kz.jyly.dto;

import kz.jyly.entity.Booking;

import java.math.BigDecimal;
import java.util.List;

public record DashboardStats(long totalBookings,
                             long pendingBookings,
                             long confirmedBookings,
                             BigDecimal revenue,
                             BigDecimal outstanding,
                             long activeRooms,
                             List<Booking> recentBookings,
                             List<DailyStat> daily,
                             int maxDailyBookings,
                             BigDecimal maxDailyRevenue) {

    /** Бір күндік статистика: құрылған брондаулар және таза түсім (сәтті төлемдер минус қайтарулар). */
    public record DailyStat(java.time.LocalDate date, long bookings, BigDecimal revenue) {
    }
}
