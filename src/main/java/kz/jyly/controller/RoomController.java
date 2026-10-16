package kz.jyly.controller;

import kz.jyly.dto.RoomSearchCriteria;
import kz.jyly.dto.RoomSearchResult;
import kz.jyly.entity.Room;
import kz.jyly.enums.RoomType;
import kz.jyly.service.BookingRules;
import kz.jyly.service.RoomService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Controller
public class RoomController {

    private final RoomService roomService;
    private final BookingRules rules;

    public RoomController(RoomService roomService, BookingRules rules) {
        this.roomService = roomService;
        this.rules = rules;
    }

    @GetMapping("/rooms")
    public String list(@ModelAttribute("criteria") RoomSearchCriteria criteria, BindingResult binding, Model model) {
        RoomSearchResult result = roomService.search(criteria);
        String error = binding.hasErrors() ? "Кейбір сүзгі мәндері дұрыс емес, олар ескерілмеді" : result.error();
        model.addAttribute("result", result);
        model.addAttribute("searchError", error);
        model.addAttribute("busyNow", roomService.busyNowIds());
        model.addAttribute("roomTypes", RoomType.values());
        return "rooms/list";
    }

    @GetMapping("/rooms/{slug}")
    public String detail(@PathVariable String slug,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         Model model) {
        Room room = roomService.getActiveBySlug(slug);
        LocalDate today = rules.today();
        LocalDate selected = date == null || date.isBefore(today) ? today : date;
        List<LocalDate> days = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            days.add(today.plusDays(i));
        }
        model.addAttribute("room", room);
        model.addAttribute("selectedDate", selected);
        model.addAttribute("days", days);
        model.addAttribute("busySlots", roomService.busySlots(room.getId(), selected));
        return "rooms/detail";
    }
}
