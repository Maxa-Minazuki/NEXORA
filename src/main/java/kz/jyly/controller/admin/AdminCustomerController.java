package kz.jyly.controller.admin;

import kz.jyly.enums.PaymentType;
import kz.jyly.enums.TransactionStatus;
import kz.jyly.service.BookingService;
import kz.jyly.service.CustomerService;
import kz.jyly.service.PaymentService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Клиенттер мен төлемдер тарихы — тек оқу үшін: әкімші төлемдерді өзгерте алмайды. */
@Controller
public class AdminCustomerController {

    private final CustomerService customerService;
    private final BookingService bookingService;
    private final PaymentService paymentService;

    public AdminCustomerController(CustomerService customerService, BookingService bookingService,
                                   PaymentService paymentService) {
        this.customerService = customerService;
        this.bookingService = bookingService;
        this.paymentService = paymentService;
    }

    @GetMapping("/admin/customers")
    public String customers(@RequestParam(required = false) String q, Model model) {
        model.addAttribute("customers", customerService.summaries(q));
        model.addAttribute("q", q);
        model.addAttribute("section", "customers");
        return "admin/customers";
    }

    @GetMapping("/admin/customers/{id}")
    public String customer(@PathVariable Long id, Model model) {
        model.addAttribute("customer", customerService.get(id));
        model.addAttribute("bookings", bookingService.forCustomer(id));
        model.addAttribute("section", "customers");
        return "admin/customer-detail";
    }

    @GetMapping("/admin/payments")
    public String payments(@RequestParam(required = false) TransactionStatus status,
                           @RequestParam(required = false) PaymentType type,
                           Model model) {
        model.addAttribute("payments", paymentService.filter(status, type));
        model.addAttribute("status", status);
        model.addAttribute("type", type);
        model.addAttribute("statuses", TransactionStatus.values());
        model.addAttribute("types", PaymentType.values());
        model.addAttribute("section", "payments");
        return "admin/payments";
    }
}
