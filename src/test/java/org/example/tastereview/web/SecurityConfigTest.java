package org.example.tastereview.web;

import static java.util.List.of;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.example.tastereview.application.AppProperties;
import org.example.tastereview.application.auth.MemberPrincipal;
import org.example.tastereview.application.comment.CommentService;
import org.example.tastereview.application.member.MemberService;
import org.example.tastereview.application.review.ReviewCommand;
import org.example.tastereview.application.review.ReviewService;
import org.example.tastereview.config.SecurityConfig;
import org.example.tastereview.domain.member.MemberRole;
import org.example.tastereview.domain.repository.ReviewImageRepository;
import org.example.tastereview.domain.review.Review;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private ReviewImageRepository reviewImageRepository;

    @MockitoBean
    private AppProperties appProperties;

    private MemberPrincipal principal() {
        return new MemberPrincipal(1L, "a@b.com", "pw", "닉네임", MemberRole.ROLE_USER);
    }

    @Test
    @DisplayName("비로그인 상태로 회원가입 화면에 접근하면 200을 반환한다")
    void givenAnonymous_whenGetSignup_thenReturns200() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/signup"));
    }

    @Test
    @DisplayName("비로그인 상태로 로그인 화면에 접근하면 200을 반환한다")
    void givenAnonymous_whenGetLogin_thenReturns200() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("비로그인 상태로 리뷰 작성 화면에 접근하면 로그인 화면으로 이동한다")
    void givenAnonymous_whenGetReviewForm_thenRedirectsToLogin() throws Exception {
        mockMvc.perform(get("/reviews/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    @DisplayName("비로그인 상태로 리뷰 목록을 조회하면 200을 반환한다")
    void givenAnonymous_whenGetReviews_thenReturns200() throws Exception {
        given(reviewService.search(any(), any(), any(), any(), anyInt()))
                .willReturn(Page.empty());
        given(appProperties.getRegions()).willReturn(of("강남구", "성동구"));
        given(appProperties.getReviewRatingMin()).willReturn(1);
        given(appProperties.getReviewRatingMax()).willReturn(5);

        mockMvc.perform(get("/reviews"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/list"));
    }

    @Test
    @DisplayName("CSRF 토큰이 없는 리뷰 등록 POST는 403을 반환한다")
    void givenNoCsrf_whenPostReview_thenReturns403() throws Exception {
        mockMvc.perform(post("/reviews")
                        .param("storeName", "성수김밥")
                        .param("region", "강남구")
                        .param("rating", "5")
                        .param("title", "맛 좋은 김밥집")
                        .param("content", "김밥이 담백하고 국물이 시원합니다. 다음에 또 방문합니다."))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증된 사용자가 CSRF와 함께 리뷰를 등록하면 생성된 상세로 이동한다")
    void givenAuthenticatedWithCsrf_whenPostReview_thenRedirectsToCreatedReview() throws Exception {
        Review review = mock(Review.class);
        given(review.getId()).willReturn(5L);
        given(reviewService.create(anyLong(), any(ReviewCommand.class))).willReturn(review);

        mockMvc.perform(post("/reviews")
                        .with(csrf())
                        .with(user(principal()))
                        .param("storeName", "성수김밥")
                        .param("region", "강남구")
                        .param("rating", "5")
                        .param("title", "맛 좋은 김밥집")
                        .param("content", "김밥이 담백하고 국물이 시원합니다. 다음에 또 방문합니다."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews/5"));
    }
}