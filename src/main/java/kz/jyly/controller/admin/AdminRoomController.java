package kz.jyly.controller.admin;

import jakarta.validation.Valid;
import kz.jyly.dto.RoomForm;
import kz.jyly.entity.Room;
import kz.jyly.enums.RoomType;
import kz.jyly.service.RoomService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/rooms")
public class AdminRoomController {

    private final RoomService roomService;

    public AdminRoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @ModelAttribute
    public void common(Model model) {
        model.addAttribute("section", "rooms");
        model.addAttribute("roomTypes", RoomType.values());
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("rooms", roomService.listAll());
        return "admin/rooms";
    }

    @GetMapping("/new")
    public String newRoom(Model model) {
        RoomForm form = new RoomForm();
        form.setType(RoomType.NETWORKING);
        model.addAttribute("roomForm", form);
        model.addAttribute("roomId", null);
        return "admin/room-form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("roomForm") RoomForm form, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("roomId", null);
            return "admin/room-form";
        }
        Room room = roomService.create(form);
        redirect.addFlashAttribute("toast", "«" + room.getName() + "» бөлмесі қосылды");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/rooms";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        model.addAttribute("roomForm", roomService.toForm(id));
        model.addAttribute("roomId", id);
        return "admin/room-form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("roomForm") RoomForm form, BindingResult binding,
                         Model model, RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("roomId", id);
            return "admin/room-form";
        }
        Room room = roomService.update(id, form);
        redirect.addFlashAttribute("toast", "«" + room.getName() + "» сақталды");
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/rooms";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id, RedirectAttributes redirect) {
        Room room = roomService.toggleActive(id);
        redirect.addFlashAttribute("toast", "«" + room.getName() + "» "
                + (room.isActive() ? "белсендірілді" : "өшірілді (тарих сақталды)"));
        redirect.addFlashAttribute("toastType", "success");
        return "redirect:/admin/rooms";
    }
}
