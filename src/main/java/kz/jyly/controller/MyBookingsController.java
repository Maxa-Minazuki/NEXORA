package kz.jyly.controller;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kz.jyly.dto.BookingDetails;
import kz.jyly.dto.BookingEditForm;
import kz.jyly.dto.BookingLookupForm;
import kz.jyly.dto.PaymentForm;
import kz.jyly.entity.Booking;
import kz.jyly.entity.Payment;
import kz.jyly.enums.PaymentType;
import kz.jyly.exception.BusinessException;
import kz.jyly.service.BookingAccessService;
import kz.jyly.service.BookingService;
import kz.jyly.service.Money;
import kz.jyly.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;

/**
 * Клиенттің "Брондау тарихы" бөлімі. Әр әрекет алдында сессияда осы брондауға
 * қолжетімділік бар-жоғы тексеріледі (нөмір + электрондық пошта арқылы расталған).
 */
@Controller
@RequestMapping("/my-bookings")
public class MyBookingsController {

    private static final String DENIED = "Бұл брондауды көру үшін алдымен нөмірі мен электрондық поштасын растаңыз";

    private final BookingService bookingService;
    private final PaymentService paymentService;
    private final BookingAccessService access;

    public MyBookingsController(BookingService bookingService, PaymentService paymentService, BookingAccessService access) {
        this.bookingService = bookingService;
        this.paymentService = paymentService;
        this.access = access;
    }

    @GetMapping
    public String list(Model model, HttpSession session) {
        if (!model.containsAttribute("lookupForm")) {
            model.addAttribute("lookupForm", new BookingLookupForm());
        }
        model.addAttribute("bookings", bookingService.findByNumbers(access.grantedNumbers(session)));
        return "my/list";
    }

    @PostMapping("/lookup")
    public String lookup(@Valid @ModelAttribute("lookupForm") BookingLookupForm form, BindingResult binding,
                         Model model, HttpSession session, RedirectAttributes redirect) {
        if (!binding.hasErrors()) {
            Optional<Booking> found = bookingService.lookup(form.getBookingNumber(), form.getEmail());
            if (found.isPresent()) {
                access.grant(session, found.get().getBookingNumber());
                toast(redirect, "Брондау табылды", "success");
                return "redirect:/my-bookings/" + found.get().getBookingNumber();
            }
            binding.reject("notFound", "Брондау табылмады. Нөмір мен электрондық поштаны тексеріп, қайта көріңіз");
        }
        model.addAttribute("bookings", bookingService.findByNumbers(access.grantedNumbers(session)));
        return "my/list";
    }

    @PostMapping("/forget")
    public String forget(HttpSession session, RedirectAttributes redirect) {
        access.forget(session);
        toast(redirect, "Брондаулар осы құрылғыдағы тізімнен жасырылды", "info");
        return "redirect:/my-bookings";
    }

    @GetMapping("/{number}")
    public String detail(@PathVariable String number, Model model, HttpSession session, RedirectAttributes redirect) {
        if (!access.hasAccess(session, number)) {
            return deny(redirect);
        }
        model.addAttribute("d", bookingService.details(number));
        return "my/detail";
    }

    @GetMapping("/{number}/pay")
    public String payForm(@PathVariable String number, @RequestParam(defaultValue = "DEPOSIT") PaymentType type,
                          Model model, HttpSession session, RedirectAttributes redirect) {
        if (!access.hasAccess(session, number)) {
            return deny(redirect);
        }
        BookingDetails d = bookingService.details(number);
        PaymentType effective = d.canPayDeposit() ? PaymentType.DEPOSIT : type;
        if ((effective == PaymentType.DEPOSIT && !d.canPayDeposit()) || (effective == PaymentType.BALANCE && !d.canPayBalance())
                || effective == PaymentType.REFUND) {
            toast(redirect, "Бұл брондау үшін қазір төлем қажет емес", "info");
            return "redirect:/my-bookings/" + d.booking().getBookingNumber();
        }
        PaymentForm form = new PaymentForm();
        form.setType(effective);
        form.setIdempotencyKey(UUID.randomUUID().toString());
        form.setAmount(effective == PaymentType.DEPOSIT
                ? d.booking().getDepositAmount().subtract(d.booking().getAmountPaid())
                : d.booking().getRemainingAmount());
        model.addAttribute("d", d);
        model.addAttribute("paymentForm", form);
        return "my/pay";
    }

    @PostMapping("/{number}/pay")
    public String pay(@PathVariable String number, @Valid @ModelAttribute("paymentForm") PaymentForm form,
                      BindingResult binding, Model model, HttpSession session, RedirectAttributes redirect) {
        if (!access.hasAccess(session, number)) {
            return deny(redirect);
        }
        BookingDetails d = bookingService.details(number);
        if (!binding.hasErrors()) {
            try {
                Payment payment = paymentService.pay(d.booking().getId(), form);
                if (payment.isSucceeded()) {
                    toast(redirect, (payment.getPaymentType() == PaymentType.DEPOSIT ? "Депозит төленді" : "Төлем қабылданды")
                            + " (демо): " + Money.format(payment.getAmount()), "success");
                } else {
                    toast(redirect, "Төлем сәтсіз аяқталды (демо). Сома есепке алынбады — қайта көріңіз", "error");
                }
                return "redirect:/my-bookings/" + d.booking().getBookingNumber();
            } catch (BusinessException e) {
                FormErrors.apply(binding, e);
            }
        }
        model.addAttribute("d", d);
        return "my/pay";
    }

    @PostMapping("/{number}/cancel")
    public String cancel(@PathVariable String number, HttpSession session, RedirectAttributes redirect) {
        if (!access.hasAccess(session, number)) {
            return deny(redirect);
        }
        try {
            Booking booking = bookingService.cancelByCustomer(number);
            toast(redirect, "Брондау тоқтатылды. Төлем тарихы сақталды", "success");
            return "redirect:/my-bookings/" + booking.getBookingNumber();
        } catch (BusinessException e) {
            toast(redirect, e.getMessage(), "error");
            return "redirect:/my-bookings/" + BookingService.normalizeNumber(number);
        }
    }

    @GetMapping("/{number}/edit")
    public String editForm(@PathVariable String number, Model model, HttpSession session, RedirectAttributes redirect) {
        if (!access.hasAccess(session, number)) {
            return deny(redirect);
        }
        BookingDetails d = bookingService.details(number);
        if (!d.canEdit()) {
            toast(redirect, "Бұл брондауды енді өзгерту мүмкін емес", "info");
            return "redirect:/my-bookings/" + d.booking().getBookingNumber();
        }
        model.addAttribute("d", d);
        model.addAttribute("editForm", bookingService.editForm(number));
        return "my/edit";
    }

    @PostMapping("/{number}/edit")
    public String edit(@PathVariable String number, @Valid @ModelAttribute("editForm") BookingEditForm form,
                       BindingResult binding, Model model, HttpSession session, RedirectAttributes redirect) {
        if (!access.hasAccess(session, number)) {
            return deny(redirect);
        }
        if (!binding.hasErrors()) {
            form.getGuests().removeIf(g -> g == null || g.isBlank());
            try {
                Booking booking = bookingService.update(number, form);
                toast(redirect, "Өзгерістер сақталды", "success");
                return "redirect:/my-bookings/" + booking.getBookingNumber();
            } catch (BusinessException e) {
                FormErrors.apply(binding, e);
            }
        }
        model.addAttribute("d", bookingService.details(number));
        return "my/edit";
    }

    private String deny(RedirectAttributes redirect) {
        toast(redirect, DENIED, "info");
        return "redirect:/my-bookings";
    }

    private static void toast(RedirectAttributes redirect, String message, String type) {
        redirect.addFlashAttribute("toast", message);
        redirect.addFlashAttribute("toastType", type);
    }
}
