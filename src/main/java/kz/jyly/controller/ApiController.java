package kz.jyly.controller;

import kz.jyly.dto.PriceQuote;
import kz.jyly.entity.Room;
import kz.jyly.exception.BusinessException;
import kz.jyly.exception.NotFoundException;
import kz.jyly.service.BookingRules;
import kz.jyly.service.BookingService;
import kz.jyly.service.Money;
import kz.jyly.service.PricingService;
import kz.jyly.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Брондау формасындағы алдын ала есептеу үшін JSON API.
 * Баға мұнда да Java-да (PricingService) есептеледі; соңғы сома брондауды сақтағанда қайта есептеледі.
 */
@RestController
@RequestMapping("/api")
public class ApiController {

    private final RoomService roomService;
    private final BookingService bookingService;
    private final PricingService pricingService;
    private final BookingRules rules;
    private final Fmt fmt;

    public ApiController(RoomService roomService, BookingService bookingService, PricingService pricingService,
                         BookingRules rules, Fmt fmt) {
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.pricingService = pricingService;
        this.rules = rules;
        this.fmt = fmt;
    }

    public record QuoteResponse(boolean ok, boolean available, String message,
                                String hourlyRate, String duration, int billableHours,
                                String total, String deposit, String balance,
                                BigDecimal depositPercent) {
    }

    public record SlotResponse(String start, String end) {
    }

    public record ErrorResponse(boolean ok, String message) {
    }

    @GetMapping("/pricing/quote")
    public QuoteResponse quote(@RequestParam Long roomId,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                               @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime end,
                               @RequestParam(required = false) Integer guests) {
        Room room = roomService.get(roomId);
        LocalDateTime from = date.atTime(start);
        LocalDateTime to = date.atTime(end);
        if (!to.isAfter(from)) {
            return new QuoteResponse(false, false, "Аяқталу уақыты басталу уақытынан кейін болуы керек",
                    Money.format(room.getHourlyRate()), null, 0, null, null, null, pricingService.getDepositPercent());
        }
        PriceQuote q = pricingService.quote(room.getHourlyRate(), from, to);
        String message = null;
        boolean ok = true;
        boolean available = false;
        try {
            if (!room.isActive()) {
                throw new BusinessException("Бұл бөлме қазір брондауға қолжетімсіз");
            }
            rules.validateInterval(from, to);
            if (guests != null) {
                rules.validateGuests(guests, room);
            }
            available = bookingService.isAvailable(room.getId(), from, to);
            if (!available) {
                ok = false;
                message = "Бұл уақыт аралығы бос емес. Басқа уақытты таңдаңыз";
            }
        } catch (BusinessException e) {
            ok = false;
            message = e.getMessage();
        }
        return new QuoteResponse(ok, available, message,
                Money.format(q.hourlyRate()), fmt.duration(q.durationMinutes()), q.billableHours(),
                Money.format(q.totalAmount()), Money.format(q.depositAmount()), Money.format(q.balanceAfterDeposit()),
                q.depositPercent());
    }

    @GetMapping("/rooms/{id}/busy")
    public List<SlotResponse> busy(@PathVariable Long id,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return roomService.busySlots(id, date).stream()
                .map(r -> new SlotResponse(fmt.time(r.start()), fmt.time(r.end())))
                .toList();
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> badRequest() {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(false, "Сұрау параметрлері дұрыс емес"));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(false, e.getMessage()));
    }
}
