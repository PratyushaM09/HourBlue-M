package com.hourblue.subscriber;

import java.util.Set;

import com.hourblue.seo.SiteUrlBuilder;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SubscriptionController {

    private static final String UNSUBSCRIBE_SUCCESS_MESSAGE = "If that email was subscribed, it has been removed.";

    private final SubscriptionService subscriptionService;
    private final SiteUrlBuilder siteUrlBuilder;
    private final Validator validator;

    public SubscriptionController(
            SubscriptionService subscriptionService,
            SiteUrlBuilder siteUrlBuilder,
            Validator validator) {
        this.subscriptionService = subscriptionService;
        this.siteUrlBuilder = siteUrlBuilder;
        this.validator = validator;
    }

    @PostMapping("/subscribe")
    String subscribe(
            @ModelAttribute("subscriptionForm") SubscriptionForm form,
            RedirectAttributes redirectAttributes) {
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "subscriptionForm");
        form.trimEmail();
        validate(form, bindingResult);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "subscriptionErrorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/";
        }

        subscriptionService.subscribe(form.getEmail());
        redirectAttributes.addFlashAttribute("subscriptionSuccessMessage", "You're on the HourBlue list.");
        return "redirect:/";
    }

    @GetMapping("/unsubscribe")
    String unsubscribe(Model model) {
        addSeo(model);
        return "public/unsubscribe";
    }

    @PostMapping("/unsubscribe")
    String unsubscribe(
            @ModelAttribute("unsubscribeForm") SubscriptionForm form,
            RedirectAttributes redirectAttributes) {
        BindingResult bindingResult = new BeanPropertyBindingResult(form, "unsubscribeForm");
        form.trimEmail();
        validate(form, bindingResult);

        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute(
                    "unsubscribeErrorMessage",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/unsubscribe";
        }

        subscriptionService.unsubscribe(form.getEmail());
        redirectAttributes.addFlashAttribute("unsubscribeSuccessMessage", UNSUBSCRIBE_SUCCESS_MESSAGE);
        return "redirect:/unsubscribe";
    }

    private void validate(SubscriptionForm form, BindingResult bindingResult) {
        Set<ConstraintViolation<SubscriptionForm>> violations = validator.validate(form);
        for (ConstraintViolation<SubscriptionForm> violation : violations) {
            bindingResult.addError(new FieldError(
                    bindingResult.getObjectName(),
                    violation.getPropertyPath().toString(),
                    violation.getMessage()));
        }
    }

    private void addSeo(Model model) {
        model.addAttribute("seoTitle", "Unsubscribe - HourBlue");
        model.addAttribute("seoDescription", "Remove an email address from the HourBlue subscription list.");
        model.addAttribute("canonicalUrl", siteUrlBuilder.canonical("/unsubscribe"));
        model.addAttribute("ogType", "website");
        model.addAttribute("ogImageUrl", null);
        model.addAttribute("ogImageAlt", null);
    }
}
