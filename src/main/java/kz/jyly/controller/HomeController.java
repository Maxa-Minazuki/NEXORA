package kz.jyly.controller;

import kz.jyly.dto.RoomSearchCriteria;
import kz.jyly.entity.Room;
import kz.jyly.service.BookingRules;
import kz.jyly.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Set;

@Controller
public class HomeController {

    private final RoomService roomService;
    private final BookingRules rules;

    public HomeController(RoomService roomService, BookingRules rules) {
        this.roomService = roomService;
        this.rules = rules;
    }

    @GetMapping("/")
    public String home(Model model) {
        List<Room> rooms = roomService.listActive();
        Set<Long> busyNow = roomService.busyNowIds();
        long freeNow = rooms.stream().filter(r -> !busyNow.contains(r.getId())).count();

        RoomSearchCriteria criteria = new RoomSearchCriteria();
        criteria.setDate(rules.today());

        model.addAttribute("rooms", rooms);
        model.addAttribute("busyNow", busyNow);
        model.addAttribute("freeNow", freeNow);
        model.addAttribute("criteria", criteria);
        return "index";
    }

    @GetMapping("/about")
    public String about(Model model) {
        model.addAttribute("roomCount", roomService.listActive().size());
        return "about";
    }
}
