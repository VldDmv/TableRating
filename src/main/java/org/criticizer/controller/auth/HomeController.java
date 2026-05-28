package org.criticizer.controller.auth;

import org.criticizer.entity.User;
import org.criticizer.security.SecurityUtil;
import org.criticizer.service.dashboard.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final SecurityUtil securityUtil;
    private final DashboardService dashboardService;

    public HomeController(SecurityUtil securityUtil, DashboardService dashboardService) {
        this.securityUtil = securityUtil;
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/", "/index"})
    public String index() {
        return "index";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        User user = securityUtil.getCurrentUser();

        var stats = dashboardService.getUserDashboardStats(user.getId());

        model.addAttribute("username", user.getName());
        model.addAttribute("gamesStats", stats.gamesStats());
        model.addAttribute("moviesStats", stats.moviesStats());
        model.addAttribute("booksStats", stats.booksStats());
        model.addAttribute("showsStats", stats.showsStats());

        return "dashboard";
    }
}
