package org.example.tastereview.web;

import static java.time.LocalDateTime.of;
import static java.util.List.of;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.time.LocalDateTime;
import java.util.List;
import org.example.tastereview.application.AppProperties;
import org.example.tastereview.application.auth.MemberPrincipal;
import org.example.tastereview.application.review.DetailView;
import org.example.tastereview.application.review.ReviewCommand;
import org.example.tastereview.application.review.ReviewService;
import org.example.tastereview.config.SecurityConfig;
import org.example.tastereview.domain.exception.ForbiddenOperationException;
import org.example.tastereview.domain.exception.ImageValidationException;
import org.example.tastereview.domain.exception.ReviewNotFoundException;
import org.example.tastereview.domain.member.MemberRole;
import org.example.tastereview.domain.review.Review;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
@Import(SecurityConfig.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private AppProperties appProperties;

    private MemberPrincipal principal() {
        return new MemberPrincipal(1L, "a@b.com", "pw", "닉네임", MemberRole.ROLE_USER);
    }

    private DetailView detailView(boolean canModify) {
        LocalDateTime createdAt = of(2026, 9, 1, 12, 0);
        return new DetailView(5L, "성수김밥", "강남구", 5, "담백한 김밥집",
                "김밥이 담백하고 국물이 시원합니다. 다음에 또 방문하고 싶은 집입니다.",
                1L, "닉네임", createdAt, createdAt, canModify, List.of(), Page.empty());
    }

    private void stubAppProperties() {
        given(appProperties.getRegions()).willReturn(of("강남구", "성동구"));
        given(appProperties.getReviewRatingMin()).willReturn(1);
        given(appProperties.getReviewRatingMax()).willReturn(5);
    }

    @Test
    @DisplayName("검색 조건을 유지한 채 목록을 렌더링한다")
    void givenStoredReviews_whenFilteredList_thenRendersListWithFilterValues() throws Exception {
        given(reviewService.search(any(), any(), any(), any(), anyInt()))
                .willReturn(Page.empty());
        stubAppProperties();

        mockMvc.perform(get("/reviews")
                        .param("keyword", "김밥")
                        .param("region", "강남구")
                        .param("minRating", "4")
                        .param("sort", "rating")
                        .param("page", "1"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/list"))
                .andExpect(model().attribute("keyword", "김밥"))
                .andExpect(model().attribute("region", "강남구"))
                .andExpect(model().attribute("minRating", 4))
                .andExpect(model().attribute("sort", "rating"));
    }

    @Test
    @DisplayName("리뷰 상세를 비로그인으로 조회하면 200을 반환한다")
    void givenStoredReview_whenDetailByGuest_thenReturns200() throws Exception {
        given(reviewService.getDetail(anyLong(), anyLong(), anyInt()))
                .willReturn(detailView(true));

        mockMvc.perform(get("/reviews/5"))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/detail"))
                .andExpect(model().attributeExists("detail"));
    }

    @Test
    @DisplayName("존재하지 않는 리뷰 상세를 요청하면 404 화면을 반환한다")
    void givenMissingReview_whenDetail_thenReturns404() throws Exception {
        given(reviewService.getDetail(anyLong(), anyLong(), anyInt()))
                .willThrow(ReviewNotFoundException.class);

        mockMvc.perform(get("/reviews/999"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/404"));
    }

    @Test
    @DisplayName("인증 후 리뷰를 등록하면 생성된 상세로 리다이렉트한다")
    void givenAuthenticated_whenCreateReview_thenRedirectsToCreatedDetail() throws Exception {
        Review review = mock(Review.class);
        given(review.getId()).willReturn(5L);
        given(reviewService.create(anyLong(), any(ReviewCommand.class))).willReturn(review);

        mockMvc.perform(post("/reviews")
                        .with(csrf())
                        .with(user(principal()))
                        .param("storeName", "성수김밥")
                        .param("region", "강남구")
                        .param("rating", "5")
                        .param("title", "담백한 김밥집")
                        .param("content", "김밥이 담백하고 국물이 시원합니다. 다음에 또 방문합니다."))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews/5"));
    }

    @Test
    @DisplayName("검증 실패가 발생하면 작성 화면을 재렌더링한다")
    void givenInvalidReview_whenCreate_thenRerendersForm() throws Exception {
        given(reviewService.create(anyLong(), any(ReviewCommand.class)))
                .willThrow(new IllegalArgumentException("제목은 1자 이상이어야 합니다"));
        stubAppProperties();

        mockMvc.perform(post("/reviews")
                        .with(csrf())
                        .with(user(principal()))
                        .param("storeName", "성수김밥")
                        .param("region", "강남구")
                        .param("rating", "5")
                        .param("title", "담백한 김밥집")
                        .param("content", "김밥이 담백하고 국물이 시원합니다. 다음에 또 방문합니다."))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/form"));
    }

    @Test
    @DisplayName("사진 검증에 실패하면 재렌더링하며 사유를 표시한다")
    void givenInvalidImage_whenCreate_thenRerendersFormWithMessage() throws Exception {
        given(reviewService.create(anyLong(), any(ReviewCommand.class)))
                .willThrow(new ImageValidationException(List.of("사진은 jpg, png, webp만 허용됩니다")));
        stubAppProperties();

        mockMvc.perform(post("/reviews")
                        .with(csrf())
                        .with(user(principal()))
                        .param("storeName", "성수김밥")
                        .param("region", "강남구")
                        .param("rating", "5")
                        .param("title", "담백한 김밥집")
                        .param("content", "김밥이 담백하고 국물이 시원합니다. 다음에 또 방문합니다."))
                .andExpect(status().isOk())
                .andExpect(view().name("reviews/form"))
                .andExpect(content().string(containsString("사진은 jpg, png, webp만 허용됩니다")));
    }

    @Test
    @DisplayName("인증 후 리뷰를 삭제하면 목록으로 리다이렉트한다")
    void givenAuthenticatedOwner_whenDeleteReview_thenRedirectsToList() throws Exception {
        mockMvc.perform(post("/reviews/5/delete")
                        .with(csrf())
                        .with(user(principal())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/reviews"));
    }

    @Test
    @DisplayName("타인의 리뷰 수정 요청이면 403 화면을 반환한다")
    void givenOtherOwner_whenUpdateReview_thenReturns403() throws Exception {
        willThrow(ForbiddenOperationException.class)
                .given(reviewService)
                .update(anyLong(), anyLong(), any(ReviewCommand.class));

        mockMvc.perform(post("/reviews/5")
                        .with(csrf())
                        .with(user(principal()))
                        .param("storeName", "성수김밥")
                        .param("region", "강남구")
                        .param("rating", "5")
                        .param("title", "담백한 김밥집")
                        .param("content", "김밥이 담백하고 국물이 시원합니다. 다음에 또 방문합니다."))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }

    @Test
    @DisplayName("타인의 리뷰 수정 화면에 접근하면 403 화면을 반환한다")
    void givenOtherOwner_whenGetEditForm_thenReturns403() throws Exception {
        given(reviewService.getDetail(anyLong(), anyLong(), anyInt()))
                .willReturn(detailView(false));

        mockMvc.perform(get("/reviews/5/edit").with(user(principal())))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error/403"));
    }
}