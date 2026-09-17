package org.example.tastereview.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.example.tastereview.application.auth.MemberPrincipal;
import org.example.tastereview.application.comment.CommentService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class CommentController {

    private static final String COMMENT_ERROR = "댓글은 1자 이상 500자 이하여야 합니다";

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping("/reviews/{reviewId}/comments")
    public String add(@PathVariable long reviewId,
                      @Valid @ModelAttribute("commentForm") CommentForm form,
                      BindingResult bindingResult,
                      @AuthenticationPrincipal MemberPrincipal principal,
                      RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return redirectWithCommentError(reviewId, redirectAttributes);
        }
        try {
            commentService.add(principal.id(), reviewId, form.getContent());
        } catch (IllegalArgumentException e) {
            return redirectWithCommentError(reviewId, redirectAttributes);
        }
        return "redirect:/reviews/" + reviewId;
    }

    @PostMapping("/reviews/{reviewId}/comments/{commentId}/delete")
    public String delete(@PathVariable long reviewId,
                         @PathVariable long commentId,
                         @AuthenticationPrincipal MemberPrincipal principal) {
        commentService.delete(principal.id(), commentId);
        return "redirect:/reviews/" + reviewId;
    }

    private String redirectWithCommentError(long reviewId,
                                            RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("commentError", COMMENT_ERROR);
        return "redirect:/reviews/" + reviewId;
    }

    public static class CommentForm {

        @NotBlank(message = "댓글을 입력해 주세요")
        @Size(max = 500, message = "댓글은 500자 이하여야 합니다")
        private String content;

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}