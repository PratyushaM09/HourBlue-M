package com.hourblue.admin;

import java.security.Principal;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
class AdminPageController {

    @GetMapping("/admin/login")
    String login(Authentication authentication) {
        if (authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/admin";
        }

        return "admin/login";
    }

    @GetMapping("/admin")
    String index(Principal principal, Model model) {
        model.addAttribute("email", principal.getName());
        return "admin/index";
    }
}
