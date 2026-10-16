package kz.jyly.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kz.jyly.dto.BookingForm;
import kz.jyly.entity.Booking;
import kz.jyly.entity.Room;
import kz.jyly.exception.BookingConflictException;
import kz.jyly.exception.BusinessException;
import kz.jyly.service.BookingAccessService;
import kz.jyly.service.BookingRules;
import kz.jyly.service.BookingService;
import kz.jyly.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
public class BookingController {

    private final BookingService bookingService;
    private final RoomService roomService;
    private final BookingAccessService accessService;
    private final BookingRules rules;

    public BookingController(BookingService bookingService, RoomService roomService,
                             BookingAccessService accessService, BookingRules rules) {
        this.bookingService = bookingService;
        this.roomService = roomService;
        this.accessService = accessService;
        this.rules = rules;
    }

    @GetMapping("/bookings/new")
    public String newBooking(@RequestParam(required = false) String room,
                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                             @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime start,
                             @RequestParam(required = false) @DateTimeFormat(pattern = "HH:mm") LocalTime end,
                             @RequestParam(required = false) Integer guests,
                             Model model) {
        List<Room> rooms = roomService.listActive();
        BookingForm form = new BookingForm();
        rooms.stream().filter(r -> r.getSlug().equals(room)).findFirst()
                .or(() -> rooms.stream().findFirst())
                .ifPresent(r -> form.setRoomId(r.getId()));
        form.setDate(date != null && !date.isBefore(rules.today()) ? date : rules.today().plusDays(1));
        form.setStartTime(start != null ? start : LocalTime.of(10, 0));
        form.setEndTime(end != null ? end : LocalTime.of(12, 0));
        form.setGuestCount(guests != null && guests > 0 ? guests : 2);
        model.addAttribute("bookingForm", form);
        model.addAttribute("rooms", rooms);
        return "bookings/new";
    }

    @PostMapping("/bookings")
    public String create(@Valid @ModelAttribute("bookingForm") BookingForm form,
                         BindingResult binding,
                         Model model,
                         HttpSession session,
                         RedirectAttributes redirect) {
        model.addAttribute("rooms", roomService.listActive());
        if (binding.hasErrors()) {
            return "bookings/new";
        }
        // Толтырылмаған бос қонақ жолдары есепке алынбайды (қате индекстері де осы тізімге сәйкес болады).
        form.getGuests().removeIf(g -> g == null || g.isBlank());
        try {
            Booking booking = bookingService.create(form);
            accessService.grant(session, booking.getBookingNumber());
            redirect.addFlashAttribute("toast", "Брондау сәтті жасалды. Нөміріңіз: " + booking.getBookingNumber());
            redirect.addFlashAttribute("toastType", "success");
            return "redirect:/my-bookings/" + booking.getBookingNumber();
        } catch (BookingConflictException e) {
            binding.reject("conflict", e.getMessage());
            long minutes = Duration.between(form.getStartTime(), form.getEndTime()).toMinutes();
            model.addAttribute("alternatives",
                    bookingService.suggestAlternatives(form.getRoomId(), form.getDate(), minutes, 4));
            model.addAttribute("conflictDuration", minutes);
            return "bookings/new";
        } catch (BusinessException e) {
            FormErrors.apply(binding, e);
            return "bookings/new";
        }
    }
}
