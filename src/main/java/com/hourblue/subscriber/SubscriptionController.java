package com.hourblue.subscriber;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final Validator validator;

    public SubscriptionController(SubscriptionService subscriptionService, Validator validator) {
        this.subscriptionService = subscriptionService;
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

    private void validate(SubscriptionForm form, BindingResult bindingResult) {
        Set<ConstraintViolation<SubscriptionForm>> violations = validator.validate(form);
        for (ConstraintViolation<SubscriptionForm> violation : violations) {
            bindingResult.addError(new FieldError(
                    "subscriptionForm",
                    violation.getPropertyPath().toString(),
                    violation.getMessage()));
        }
    }
}
