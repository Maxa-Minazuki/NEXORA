package kz.jyly.controller.admin;

import kz.jyly.enums.BookingStatus;
import kz.jyly.exception.BusinessException;
import kz.jyly.service.BookingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;

@Controller
@RequestMapping("/admin/bookings")
public class AdminBookingController {

    private final BookingService bookingService;

    public AdminBookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) BookingStatus status,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model) {
        model.addAttribute("bookings", bookingService.adminSearch(q, status, from, to));
        model.addAttribute("q", q);
        model.addAttribute("status", status);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("statuses", BookingStatus.values());
        model.addAttribute("section", "bookings");
        return "admin/bookings";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("d", bookingService.detailsById(id));
        model.addAttribute("section", "bookings");
        return "admin/booking-detail";
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam BookingStatus target, RedirectAttributes redirect) {
        try {
            bookingService.changeStatusByAdmin(id, target);
            redirect.addFlashAttribute("toast", "Статус өзгертілді: " + target.getLabel());
            redirect.addFlashAttribute("toastType", "success");
        } catch (BusinessException e) {
            redirect.addFlashAttribute("toast", e.getMessage());
            redirect.addFlashAttribute("toastType", "error");
        }
        return "redirect:/admin/bookings/" + id;
    }
}
