package kz.jyly.controller;

import jakarta.servlet.http.HttpServletRequest;
import kz.jyly.config.JylyProperties;
import kz.jyly.service.BookingRules;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Барлық HTML беттеріне ортақ модель атрибуттары. */
@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAttributes {

    private static final DateTimeFormatter HH_MM = DateTimeFormatter.ofPattern("HH:mm");

    private final BookingRules rules;

    public GlobalModelAttributes(BookingRules rules) {
        this.rules = rules;
    }

    @ModelAttribute("currentPath")
    public String currentPath(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("club")
    public JylyProperties club() {
        return rules.getProperties();
    }

    @ModelAttribute("timeSlots")
    public List<String> timeSlots() {
        return rules.timeSlots().stream().map((LocalTime t) -> t.format(HH_MM)).toList();
    }

    @ModelAttribute("today")
    public String today() {
        return rules.today().toString();
    }

    @ModelAttribute("maxDate")
    public String maxDate() {
        return rules.today().plusDays(rules.getProperties().getMaxDaysAhead()).toString();
    }

    @ModelAttribute("now")
    public LocalDateTime now() {
        return rules.now();
    }

    @ModelAttribute("currentYear")
    public int currentYear() {
        return rules.today().getYear();
    }
}
