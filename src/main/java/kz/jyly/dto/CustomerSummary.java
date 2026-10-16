package kz.jyly.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerSummary(Long id,
                              String firstName,
                              String lastName,
                              String email,
                              String phone,
                              String company,
                              LocalDateTime createdAt,
                              long bookingCount,
                              BigDecimal totalPaid) {
}
