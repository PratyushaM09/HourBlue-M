package com.hourblue.admin.today;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import com.hourblue.today.InvalidTodayMomentException;
import com.hourblue.today.TodayMoment;
import com.hourblue.today.TodayMomentService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AdminTodayMomentController {

    private final TodayMomentService todayMomentService;

    public AdminTodayMomentController(TodayMomentService todayMomentService) {
        this.todayMomentService = todayMomentService;
    }

    @GetMapping("/admin/today")
    String index(@RequestParam(required = false) String date, Model model) {
        LocalDate featureDate = parseDate(date, model);
        addPageAttributes(model, featureDate, null);
        return "admin/today";
    }

    @PostMapping("/admin/today")
    String assign(
            @RequestParam(required = false) String featureDate,
            @RequestParam(required = false) Long postId,
            Model model,
            RedirectAttributes redirectAttributes) {
        LocalDate parsedDate = parseDate(featureDate, model);
        if (model.containsAttribute("errorMessage")) {
            addPageAttributes(model, parsedDate, null);
            return "admin/today";
        }

        try {
            todayMomentService.assign(parsedDate, postId);
            redirectAttributes.addAttribute("date", parsedDate.toString());
            return "redirect:/admin/today";
        } catch (InvalidTodayMomentException exception) {
            addPageAttributes(model, parsedDate, exception.getMessage());
            return "admin/today";
        }
    }

    private LocalDate parseDate(String value, Model model) {
        if (value == null || value.isBlank()) {
            return todayMomentService.today();
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            model.addAttribute("errorMessage", "Date must use YYYY-MM-DD.");
            return todayMomentService.today();
        }
    }

    private void addPageAttributes(Model model, LocalDate featureDate, String errorMessage) {
        if (errorMessage != null) {
            model.addAttribute("errorMessage", errorMessage);
        }
        TodayMoment todayMoment = todayMomentService.findAssignment(featureDate).orElse(null);
        model.addAttribute("dateValue", featureDate);
        model.addAttribute("todayMoment", todayMoment);
        model.addAttribute("publishedPosts", todayMomentService.eligiblePosts());
    }
}
