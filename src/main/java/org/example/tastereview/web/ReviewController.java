package org.example.tastereview.web;

import jakarta.validation.Valid;
import org.example.tastereview.application.AppProperties;
import org.example.tastereview.application.auth.MemberPrincipal;
import org.example.tastereview.application.review.DetailView;
import org.example.tastereview.application.review.ReviewCommand;
import org.example.tastereview.application.review.ReviewService;
import org.example.tastereview.application.review.SummaryView;
import org.example.tastereview.domain.exception.ForbiddenOperationException;
import org.example.tastereview.domain.exception.ImageValidationException;
import org.example.tastereview.domain.review.Review;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class ReviewController {

    private static final String CREATE_MODE = "create";
    private static final String EDIT_MODE = "edit";

    private final ReviewService reviewService;
    private final AppProperties appProperties;

    public ReviewController(ReviewService reviewService, AppProperties appProperties) {
        this.reviewService = reviewService;
        this.appProperties = appProperties;
    }

    @GetMapping("/reviews")
    public String list(@RequestParam(required = false) String keyword,
                       @RequestParam(required = false) String region,
                       @RequestParam(name = "minRating", required = false) Integer minRating,
                       @RequestParam(required = false) String sort,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal MemberPrincipal principal,
                       Model model) {
        Page<SummaryView> reviews = reviewService.search(keyword, region, minRating, sort, page);
        model.addAttribute("reviews", reviews);
        model.addAttribute("keyword", keyword);
        model.addAttribute("region", region);
        model.addAttribute("minRating", minRating);
        model.addAttribute("sort", sort);
        model.addAttribute("regions", appProperties.getRegions());
        model.addAttribute("ratingMin", appProperties.getReviewRatingMin());
        model.addAttribute("ratingMax", appProperties.getReviewRatingMax());
        model.addAttribute("isAuthenticated", principal != null);
        return "reviews/list";
    }

    @GetMapping("/reviews/new")
    public String createForm(Model model) {
        model.addAttribute("command", new ReviewCommand());
        addFormModel(model, CREATE_MODE, null);
        return "reviews/form";
    }

    @PostMapping("/reviews")
    public String create(@Valid @ModelAttribute("command") ReviewCommand command,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal MemberPrincipal principal,
                         Model model) {
        if (bindingResult.hasErrors()) {
            addFormModel(model, CREATE_MODE, null);
            return "reviews/form";
        }
        try {
            Review created = reviewService.create(principal.id(), command);
            return "redirect:/reviews/" + created.getId();
        } catch (IllegalArgumentException e) {
            bindingResult.reject("review.invalid",
                    e.getMessage() != null ? e.getMessage() : "입력값이 올바르지 않습니다");
            addFormModel(model, CREATE_MODE, null);
            return "reviews/form";
        } catch (ImageValidationException e) {
            bindingResult.reject("image.invalid",
                    e.getMessage() != null ? e.getMessage() : "사진이 올바르지 않습니다");
            addFormModel(model, CREATE_MODE, null);
            return "reviews/form";
        }
    }

    @GetMapping("/reviews/{id}")
    public String detail(@PathVariable long id,
                         @RequestParam(name = "page", defaultValue = "0") int p,
                         @AuthenticationPrincipal MemberPrincipal principal,
                         Model model) {
        long viewerId = principal == null ? -1 : principal.id();
        DetailView detail = reviewService.getDetail(id, viewerId, p);
        model.addAttribute("detail", detail);
        model.addAttribute("commentPage", p);
        model.addAttribute("ratingMin", appProperties.getReviewRatingMin());
        model.addAttribute("ratingMax", appProperties.getReviewRatingMax());
        model.addAttribute("isAuthenticated", principal != null);
        return "reviews/detail";
    }

    @GetMapping("/reviews/{id}/edit")
    public String editForm(@PathVariable long id,
                           @AuthenticationPrincipal MemberPrincipal principal,
                           Model model) {
        DetailView detail = reviewService.getDetail(id, principal.id(), 0);
        if (!detail.canModify()) {
            throw new ForbiddenOperationException();
        }
        ReviewCommand command = new ReviewCommand();
        command.setStoreName(detail.storeName());
        command.setRegion(detail.region());
        command.setRating(detail.rating());
        command.setTitle(detail.title());
        command.setContent(detail.content());
        model.addAttribute("command", command);
        model.addAttribute("isAuthenticated", true);
        addFormModel(model, EDIT_MODE, id);
        return "reviews/form";
    }

    @PostMapping("/reviews/{id}")
    public String update(@PathVariable long id,
                         @Valid @ModelAttribute("command") ReviewCommand command,
                         BindingResult bindingResult,
                         @AuthenticationPrincipal MemberPrincipal principal,
                         Model model) {
        addFormModel(model, EDIT_MODE, id);
        if (bindingResult.hasErrors()) {
            return "reviews/form";
        }
        try {
            reviewService.update(principal.id(), id, command);
            return "redirect:/reviews/" + id;
        } catch (IllegalArgumentException e) {
            bindingResult.reject("review.invalid",
                    e.getMessage() != null ? e.getMessage() : "입력값이 올바르지 않습니다");
            return "reviews/form";
        } catch (ImageValidationException e) {
            bindingResult.reject("image.invalid",
                    e.getMessage() != null ? e.getMessage() : "사진이 올바르지 않습니다");
            return "reviews/form";
        }
    }

    @GetMapping("/reviews/{id}/delete")
    public String deleteForm(@PathVariable long id,
                             @AuthenticationPrincipal MemberPrincipal principal,
                             Model model) {
        DetailView detail = reviewService.getDetail(id, principal.id(), 0);
        if (!detail.canModify()) {
            throw new ForbiddenOperationException();
        }
        model.addAttribute("reviewId", id);
        model.addAttribute("storeName", detail.storeName());
        model.addAttribute("title", detail.title());
        model.addAttribute("isAuthenticated", true);
        return "reviews/delete";
    }

    @PostMapping("/reviews/{id}/delete")
    public String delete(@PathVariable long id,
                         @AuthenticationPrincipal MemberPrincipal principal) {
        reviewService.delete(principal.id(), id);
        return "redirect:/reviews";
    }

    private void addFormModel(Model model, String mode, Long reviewId) {
        model.addAttribute("regions", appProperties.getRegions());
        model.addAttribute("ratingMin", appProperties.getReviewRatingMin());
        model.addAttribute("ratingMax", appProperties.getReviewRatingMax());
        model.addAttribute("mode", mode);
        if (reviewId != null) {
            model.addAttribute("reviewId", reviewId);
        }
    }
}