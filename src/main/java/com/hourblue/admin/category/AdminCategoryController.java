package com.hourblue.admin.category;

import java.util.Set;

import com.hourblue.category.Category;
import com.hourblue.category.CategoryRepository;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;
    private final Validator validator;

    public AdminCategoryController(CategoryRepository categoryRepository, Validator validator) {
        this.categoryRepository = categoryRepository;
        this.validator = validator;
    }

    @GetMapping("/admin/categories")
    String index(Model model) {
        if (!model.containsAttribute("categoryForm")) {
            model.addAttribute("categoryForm", new CategoryForm());
        }
        addCategories(model);
        return "admin/categories";
    }

    @PostMapping("/admin/categories")
    String create(
            @ModelAttribute("categoryForm") CategoryForm form,
            BindingResult bindingResult,
            Model model) {
        form.normalize();
        validate(form, bindingResult);
        rejectDuplicates(form, bindingResult);

        if (bindingResult.hasErrors()) {
            addCategories(model);
            return "admin/categories";
        }

        categoryRepository.save(new Category(form.getName(), form.getSlug(), form.getDescription()));
        return "redirect:/admin/categories";
    }

    private void validate(CategoryForm form, BindingResult bindingResult) {
        Set<ConstraintViolation<CategoryForm>> violations = validator.validate(form);
        for (ConstraintViolation<CategoryForm> violation : violations) {
            bindingResult.addError(new FieldError(
                    "categoryForm",
                    violation.getPropertyPath().toString(),
                    violation.getMessage()));
        }
    }

    private void rejectDuplicates(CategoryForm form, BindingResult bindingResult) {
        if (!bindingResult.hasFieldErrors("name") && categoryRepository.existsByName(form.getName())) {
            bindingResult.rejectValue("name", "duplicate", "Category name already exists.");
        }
        if (!bindingResult.hasFieldErrors("slug") && categoryRepository.existsBySlug(form.getSlug())) {
            bindingResult.rejectValue("slug", "duplicate", "Category slug already exists.");
        }
    }

    private void addCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAllByOrderByNameAsc());
    }
}
