package org.example.tastereview.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.example.tastereview.application.auth.MemberPrincipal;
import org.example.tastereview.application.comment.CommentService;
import org.example.tastereview.config.SecurityConfig;
import org.example.tastereview.domain.exception.ForbiddenOperationException;
import org.example.tastereview.domain.member.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CommentController.class)
@Import(SecurityConfig.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    private MemberPrincipal principal() {
        return new MemberPrincipal(1L, "a@b.com", "pw", "닉네임", MemberRole.ROLE_USER);
    }

    @Test
    @DisplayName("로그인 사용자가 댓글을 등록하면 해당 리뷰로 리다이렉트한다")
    void givenAuthenticated_whenAddComment_thenRedirectsToReview() throws Exception {
        mockMvc.perform(post("/reviews/5/comments")
                        .with(csrf())
                        .with(user(principal()))
                        .param("content", "주차 가능한가요?"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews/5"));
    }

    @Test
    @DisplayName("댓글이 비어 있으면 flash 에러를 담아 리다이렉트한다")
    void givenEmptyContent_whenAddComment_thenRedirectsWithFlashError() throws Exception {
        mockMvc.perform(post("/reviews/5/comments")
                        .with(csrf())
                        .with(user(principal()))
                        .param("content", ""))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews/5"))
                .andExpect(flash().attribute("commentError", "댓글은 1자 이상 500자 이하여야 합니다"));
    }

    @Test
    @DisplayName("댓글 등록 중 검증 예외가 발생해도 같은 안내로 리다이렉트한다")
    void givenServiceRejects_whenAddComment_thenRedirectsWithFlashError() throws Exception {
        willThrow(new IllegalArgumentException())
                .given(commentService)
                .add(anyLong(), anyLong(), anyString());

        mockMvc.perform(post("/reviews/5/comments")
                        .with(csrf())
                        .with(user(principal()))
                        .param("content", "주차 가능한가요?"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews/5"))
                .andExpect(flash().attribute("commentError", "댓글은 1자 이상 500자 이하여야 합니다"));
    }

    @Test
    @DisplayName("로그인 사용자가 자신의 댓글을 삭제하면 리뷰로 리다이렉트한다")
    void givenAuthenticatedOwner_whenDeleteComment_thenRedirectsToReview() throws Exception {
        mockMvc.perform(post("/reviews/5/comments/9/delete")
                        .with(csrf())
                        .with(user(principal())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews/5"));
    }

    @Test
    @DisplayName("타인의 댓글 삭제 요청이면 403 화면을 반환한다")
    void givenOtherOwner_whenDeleteComment_thenReturns403() throws Exception {
        willThrow(ForbiddenOperationException.class)
                .given(commentService)
                .delete(anyLong(), anyLong());

        mockMvc.perform(post("/reviews/5/comments/9/delete")
                        .with(csrf())
                        .with(user(principal())))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }
}