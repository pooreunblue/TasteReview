package org.example.tastereview.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import org.example.tastereview.application.auth.MemberPrincipal;
import org.example.tastereview.domain.comment.Comment;
import org.example.tastereview.domain.member.Member;
import org.example.tastereview.domain.member.MemberRole;
import org.example.tastereview.domain.repository.CommentRepository;
import org.example.tastereview.domain.repository.MemberRepository;
import org.example.tastereview.domain.repository.ReviewImageRepository;
import org.example.tastereview.domain.repository.ReviewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TasteReviewMvpIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    MockMvc mockMvc;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    ReviewRepository reviewRepository;

    @Autowired
    CommentRepository commentRepository;

    @Autowired
    ReviewImageRepository reviewImageRepository;

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        reviewImageRepository.deleteAll();
        reviewRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("회원 가입 후 실제 폼 로그인, 리뷰 등록, 상세 조회 플로우가 완결된다")
    void givenGuest_whenSignupLoginAndCreateReview_thenDetailShowsReviewAndImage() throws Exception {
        String email = "gourmet@example.com";
        signup(email, "passw0rd1", "푸디");

        mockMvc.perform(post("/login")
                .param("username", email)
                .param("password", "passw0rd1")
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/reviews"));

        Long reviewId = createReview(loginAs(email),
            "성수 푸디하우스", "성동구", 5,
            "성수 푸디하우스 후기", "정말 맛있었습니다. 재방문 의사 있습니다.");

        mockMvc.perform(get("/reviews/" + reviewId))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("성수 푸디하우스 후기")))
            .andExpect(content().string(containsString("5")))
            .andExpect(content().string(containsString("<img")))
            .andExpect(content().string(containsString("/images/")));
    }

    @Test
    @DisplayName("지역·별점 필터와 키워드 검색, 별점순 정렬이 동작한다")
    void givenThreeReviews_whenSearchFilterAndSort_thenExpectedSubsetAndOrder() throws Exception {
        String email = "reviewer@example.com";
        signup(email, "passw0rd1", "리뷰어");
        UserDetails member = loginAs(email);
        createReview(member, "강남푸디", "강남구", 5, "강남 최고 푸디집", "강남에서 만난 맛집입니다.");
        createReview(member, "송파식당", "강남구", 3, "조용한 송파 식당", "평범한 한 끼였습니다.");
        createReview(member, "마포골목식당", "마포구", 4,
            "마포 골목 순대", "골목 구석 순대 맛집입니다.");

        mockMvc.perform(get("/reviews")
                .param("region", "강남구")
                .param("minRating", "5"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("강남푸디")))
            .andExpect(content().string(not(containsString("송파식당"))))
            .andExpect(content().string(not(containsString("마포골목식당"))));

        mockMvc.perform(get("/reviews").param("keyword", "푸디"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("강남푸디")))
            .andExpect(content().string(not(containsString("송파식당"))))
            .andExpect(content().string(not(containsString("마포골목식당"))));

        String body = mockMvc.perform(get("/reviews").param("sort", "rating"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(body.indexOf("강남푸디")).isLessThan(body.indexOf("마포골목식당"));
        assertThat(body.indexOf("마포골목식당")).isLessThan(body.indexOf("송파식당"));
    }

    @Test
    @DisplayName("댓글을 등록하면 상세에 노출되고 작성자가 삭제하면 사라진다")
    void givenComment_whenDeletedByAuthor_thenRemovedFromDetail() throws Exception {
        String ownerEmail = "owner@example.com";
        signup(ownerEmail, "passw0rd1", "주인장");
        Long reviewId = createReview(loginAs(ownerEmail),
            "강남식당", "강남구", 4, "강남 한정식", "오랜만에 제대로 먹었습니다.");

        mockMvc.perform(post("/reviews/" + reviewId + "/comments")
                .param("content", "주차 가능한가요?")
                .with(csrf())
                .with(user(loginAs(ownerEmail))))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/reviews/" + reviewId))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("주차 가능한가요?")));

        Comment comment = commentRepository.findAll().stream()
            .filter(c -> c.getContent().equals("주차 가능한가요?"))
            .findFirst()
            .orElseThrow();

        mockMvc.perform(post("/reviews/" + reviewId + "/comments/" + comment.getId() + "/delete")
                .with(csrf())
                .with(user(loginAs(ownerEmail))))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/reviews/" + reviewId))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("주차 가능한가요?"))));
    }

    @Test
    @DisplayName("비로그인 작성 접근, 타인 리뷰 삭제, CSRF 누락 요청이 거부된다")
    void givenGuestAndOtherMember_whenViolatingPermission_thenAccessDenied() throws Exception {
        mockMvc.perform(get("/reviews/new"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("/login")));

        String ownerEmail = "owner@example.com";
        signup(ownerEmail, "passw0rd1", "주인장");
        Long reviewId = createReview(loginAs(ownerEmail),
            "강남식당", "강남구", 4, "강남 한정식", "오랜만에 제대로 먹었습니다.");

        String otherEmail = "other@example.com";
        signup(otherEmail, "passw0rd1", "구경꾼");
        mockMvc.perform(post("/reviews/" + reviewId + "/delete")
                .with(csrf())
                .with(user(loginAs(otherEmail))))
            .andExpect(status().isForbidden());

        mockMvc.perform(multipart("/reviews")
                .file(new MockMultipartFile("images", "photo.jpg", "image/jpeg", new byte[]{1}))
                .param("storeName", "무단식당")
                .param("region", "강남구")
                .param("rating", "4")
                .param("title", "무단 등록")
                .param("content", "CSRF 없는 요청은 거부됩니다.")
                .with(user(loginAs(ownerEmail))))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("리뷰 삭제 시 사진과 댓글이 함께 제거된다")
    void givenReviewWithImageAndComment_whenDeleteReview_thenRelatedDataRemoved() throws Exception {
        String email = "owner@example.com";
        signup(email, "passw0rd1", "주인장");
        UserDetails owner = loginAs(email);
        Long reviewId = createReview(owner,
            "강남식당", "강남구", 4, "강남 한정식", "오랜만에 제대로 먹었습니다.");

        mockMvc.perform(post("/reviews/" + reviewId + "/comments")
                .param("content", "따라갈 댓글")
                .with(csrf())
                .with(user(owner)))
            .andExpect(status().is3xxRedirection());

        mockMvc.perform(post("/reviews/" + reviewId + "/delete")
                .with(csrf())
                .with(user(owner)))
            .andExpect(status().is3xxRedirection());

        assertThat(reviewRepository.findById(reviewId)).isEmpty();
        assertThat(reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(reviewId)).isEmpty();
        assertThat(commentRepository.findPageByReviewId(reviewId, Pageable.unpaged()).getTotalElements()).isZero();
    }

    private void signup(String email, String password, String nickname) throws Exception {
        mockMvc.perform(post("/signup")
                .param("email", email)
                .param("password", password)
                .param("nickname", nickname)
                .with(csrf()))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/login?registered"));
    }

    private UserDetails loginAs(String email) {
        Member member = memberRepository.findByEmail(email).orElseThrow();
        return new MemberPrincipal(
            member.getId(),
            member.getEmail(),
member.getPasswordHash(),
        member.getNickname(),
        MemberRole.ROLE_USER
    );
    }

    private Long createReview(UserDetails principal, String storeName, String region, int rating,
                              String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/reviews")
                .file(new MockMultipartFile("images", "photo.jpg", "image/jpeg", new byte[]{1, 2, 3}))
                .param("storeName", storeName)
                .param("region", region)
                .param("rating", String.valueOf(rating))
                .param("title", title)
                .param("content", content)
                .with(csrf())
                .with(user(principal)))
            .andExpect(status().is3xxRedirection())
            .andReturn();
        String location = result.getResponse().getRedirectedUrl();
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }
}