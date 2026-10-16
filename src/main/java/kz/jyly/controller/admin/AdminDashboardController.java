package kz.jyly.controller.admin;

import kz.jyly.service.AdminStatsService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminDashboardController {

    private final AdminStatsService statsService;

    public AdminDashboardController(AdminStatsService statsService) {
        this.statsService = statsService;
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        model.addAttribute("stats", statsService.dashboard());
        model.addAttribute("section", "dashboard");
        return "admin/dashboard";
    }
}
