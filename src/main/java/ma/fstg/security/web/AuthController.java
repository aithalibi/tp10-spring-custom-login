package ma.fstg.security.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/home")
    public String home(Authentication authentication, Model model) {
        model.addAttribute("utilisateur", authentication.getName());
        model.addAttribute("autorites", authentication.getAuthorities());
        return "home";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Authentication authentication, Model model) {
        model.addAttribute("utilisateur", authentication.getName());
        return "admin-dashboard";
    }

    @GetMapping("/user/dashboard")
    public String userDashboard(Authentication authentication, Model model) {
        model.addAttribute("utilisateur", authentication.getName());
        model.addAttribute("autorites", authentication.getAuthorities());
        return "user-dashboard";
    }
}