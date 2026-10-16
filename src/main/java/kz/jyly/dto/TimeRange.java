package kz.jyly.dto;

import java.time.LocalDateTime;

/** Бөлменің бос емес уақыты (жеке деректерсіз). */
public record TimeRange(LocalDateTime start, LocalDateTime end) {
}
