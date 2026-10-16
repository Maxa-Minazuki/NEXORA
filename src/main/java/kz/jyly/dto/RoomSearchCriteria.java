package kz.jyly.dto;

import kz.jyly.enums.RoomType;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

/** Каталогтағы іздеу мен сүзгілер (GET параметрлері). */
public class RoomSearchCriteria {

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime start;

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime end;

    private Integer guests;

    private BigDecimal maxPrice;

    private RoomType type;

    /** price_asc, price_desc, capacity_asc, capacity_desc */
    private String sort = "price_asc";

    public boolean hasAnyInterval() {
        return date != null || start != null || end != null;
    }

    public boolean hasFullInterval() {
        return date != null && start != null && end != null;
    }

    public boolean isFiltered() {
        return hasAnyInterval() || guests != null || maxPrice != null || type != null;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }
    public LocalTime getStart() { return start; }
    public void setStart(LocalTime start) { this.start = start; }
    public LocalTime getEnd() { return end; }
    public void setEnd(LocalTime end) { this.end = end; }
    public Integer getGuests() { return guests; }
    public void setGuests(Integer guests) { this.guests = guests; }
    public BigDecimal getMaxPrice() { return maxPrice; }
    public void setMaxPrice(BigDecimal maxPrice) { this.maxPrice = maxPrice; }
    public RoomType getType() { return type; }
    public void setType(RoomType type) { this.type = type; }
    public String getSort() { return sort; }
    public void setSort(String sort) { this.sort = sort; }
}
