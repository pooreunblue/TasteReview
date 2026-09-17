package org.example.tastereview.application.review;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import org.example.tastereview.application.AppProperties;
import org.example.tastereview.domain.comment.Comment;
import org.example.tastereview.domain.exception.ForbiddenOperationException;
import org.example.tastereview.domain.exception.ImageValidationException;
import org.example.tastereview.domain.member.Member;
import org.example.tastereview.domain.repository.CommentRepository;
import org.example.tastereview.domain.repository.MemberRepository;
import org.example.tastereview.domain.repository.ReviewImageRepository;
import org.example.tastereview.domain.repository.ReviewRepository;
import org.example.tastereview.domain.review.Review;
import org.example.tastereview.domain.review.ReviewImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ReviewImageRepository reviewImageRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ImageValidator imageValidator;

    private final AppProperties appProperties = new AppProperties();

    private ReviewService reviewService() {
        return new ReviewService(reviewRepository, reviewImageRepository, commentRepository,
                memberRepository, appProperties, imageValidator);
    }

    private Member member(long id) {
        Member member = new Member("a@b.com", "hash", "닉네임");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Review review(long id, Member author) {
        Review review = new Review("맛집", "강남구", 5, "제목", "맛있는 가게입니다", author);
        ReflectionTestUtils.setField(review, "id", id);
        return review;
    }

    private ReviewCommand command(String region, int rating, List<MultipartFile> images) {
        ReviewCommand command = new ReviewCommand();
        command.setStoreName("맛집");
        command.setRegion(region);
        command.setRating(rating);
        command.setTitle("제목");
        command.setContent("정말 맛있는 가게였어요");
        command.setImages(images);
        return command;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Specification<Review>> captorForSpecification() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(Specification.class);
    }

    @SuppressWarnings("unchecked")
    private void stubCriteria(Root<Review> root, CriteriaBuilder cb) {
        Path<Object> path = mock(Path.class);
        given(root.get(anyString())).willReturn(path);
    }

    @SuppressWarnings("unchecked")
    private Expression<String> stubLower(CriteriaBuilder cb) {
        Expression<String> lower = mock(Expression.class);
        given(cb.lower(any(Expression.class))).willReturn(lower);
        return lower;
    }

    @Test
    @DisplayName("작성자가 주어지면 이미지와 함께 리뷰를 저장한다")
    void givenAuthorAndImage_whenCreate_thenReviewSavedWithImage() {
        Member author = member(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(author));
        given(reviewRepository.save(any(Review.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile image = new MockMultipartFile(
                "images", "a.jpg", "image/jpeg", new byte[]{1, 2, 3});

        Review saved = reviewService().create(1L, command("강남구", 5, List.of(image)));

        assertThat(saved.getStoreName()).isEqualTo("맛집");
        assertThat(saved.getRating()).isEqualTo(5);
        assertThat(saved.getImages()).hasSize(1);
        assertThat(saved.getImages().get(0).getOriginalFileName()).isEqualTo("a.jpg");
        assertThat(saved.getImages().get(0).getDisplayOrder()).isZero();
        assertThat(saved.getImages().get(0).getReview()).isSameAs(saved);
        verify(reviewRepository).save(any(Review.class));
    }

    @Test
    @DisplayName("빈 파일은 제외하고 사진을 저장한다")
    void givenBlankImages_whenCreate_thenBlankIgnored() {
        Member author = member(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(author));
        given(reviewRepository.save(any(Review.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile empty = new MockMultipartFile(
                "images", "empty.jpg", "image/jpeg", new byte[0]);
        MockMultipartFile valid = new MockMultipartFile(
                "images", "a.jpg", "image/jpeg", new byte[]{1});

        Review saved = reviewService().create(1L, command("강남구", 5, List.of(empty, valid)));

        assertThat(saved.getImages()).hasSize(1);
        assertThat(saved.getImages().get(0).getOriginalFileName()).isEqualTo("a.jpg");
    }

    @Test
    @DisplayName("이미지 검증에 실패하면 리뷰를 저장하지 않는다")
    void givenImageValidationFailure_whenCreate_thenReviewNotSaved() {
        Member author = member(1L);
        given(memberRepository.findById(1L)).willReturn(Optional.of(author));
        ImageValidationException violation =
                new ImageValidationException(List.of("사진을 다시 확인해 주세요"));
        willThrow(violation).given(imageValidator).validate(anyList());

        assertThatThrownBy(() -> reviewService().create(1L, command("강남구", 5, List.of())))
                .isInstanceOf(ImageValidationException.class)
                .hasMessage("사진을 다시 확인해 주세요");
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("타인이 수정하려 하면 금지 예외가 발생하고 저장하지 않는다")
    void givenNotOwner_whenUpdate_thenForbiddenOperationException() {
        Member author = member(1L);
        given(reviewRepository.findDetailById(9L))
                .willReturn(Optional.of(review(9L, author)));

        assertThatThrownBy(
                () -> reviewService().update(2L, 9L, command("강남구", 5, List.of())))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    @DisplayName("새 사진이 있으면 수정 시 기존 사진을 교체한다")
    void givenNewImages_whenUpdate_thenExistingImagesReplaced() {
        Member author = member(1L);
        Review existing = review(9L, author);
        ReviewImage oldImage =
                new ReviewImage(existing, "image/jpeg", "old.jpg", 0, new byte[]{1});
        existing.addImage(oldImage);
        given(reviewRepository.findDetailById(9L)).willReturn(Optional.of(existing));
        given(reviewRepository.save(any(Review.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile newImage = new MockMultipartFile(
                "images", "new.jpg", "image/jpeg", new byte[]{2});

        reviewService().update(1L, 9L, command("강남구", 4, List.of(newImage)));

        assertThat(existing.getRating()).isEqualTo(4);
        assertThat(existing.getImages()).hasSize(1);
        assertThat(existing.getImages().get(0).getOriginalFileName()).isEqualTo("new.jpg");
        assertThat(existing.getImages().get(0).getDisplayOrder()).isZero();
    }

    @Test
    @DisplayName("새 사진이 없으면 수정 시 기존 사진을 유지한다")
    void givenNoNewImages_whenUpdate_thenExistingImagesKept() {
        Member author = member(1L);
        Review existing = review(9L, author);
        ReviewImage oldImage =
                new ReviewImage(existing, "image/jpeg", "old.jpg", 0, new byte[]{1});
        existing.addImage(oldImage);
        given(reviewRepository.findDetailById(9L)).willReturn(Optional.of(existing));
        given(reviewRepository.save(any(Review.class)))
                .willAnswer(invocation -> invocation.getArgument(0));
        MockMultipartFile empty = new MockMultipartFile(
                "images", "empty.jpg", "image/jpeg", new byte[0]);

        reviewService().update(1L, 9L, command("강남구", 5, List.of(empty)));

        assertThat(existing.getImages())
                .extracting(ReviewImage::getOriginalFileName)
                .containsExactly("old.jpg");
    }

    @Test
    @DisplayName("삭제 시 댓글을 먼저 지우고 리뷰를 삭제한다")
    void givenOwner_whenDelete_thenCommentsRemovedThenReviewDeleted() {
        Member author = member(1L);
        Review existing = review(9L, author);
        given(reviewRepository.findDetailById(9L)).willReturn(Optional.of(existing));

        reviewService().delete(1L, 9L);

        then(commentRepository).should().deleteAllByReviewId(9L);
        then(reviewRepository).should().delete(existing);
    }

    @Test
    @DisplayName("상세 조회는 작성자 여부와 이미지·댓글 페이지를 담아 반환한다")
    void givenReview_whenGetDetail_thenDetailWithCanModifyImagesAndComments() {
        Member author = member(1L);
        Review existing = review(9L, author);
        given(reviewRepository.findDetailById(9L)).willReturn(Optional.of(existing));
        ReviewImage image =
                new ReviewImage(existing, "image/jpeg", "a.jpg", 0, new byte[]{1});
        ReflectionTestUtils.setField(image, "id", 1L);
        given(reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(9L))
                .willReturn(List.of(image));
        Member commenter = member(2L);
        Comment comment = new Comment(commenter, existing, "댓글 내용");
        ReflectionTestUtils.setField(comment, "id", 3L);
        Page<Comment> commentPage = new PageImpl<>(List.of(comment),
                PageRequest.of(1, 20, Sort.by("createdAt").ascending()), 1);
        given(commentRepository.findPageByReviewId(eq(9L), any(Pageable.class)))
                .willReturn(commentPage);

        DetailView detail = reviewService().getDetail(9L, 1L, 1);

        assertThat(detail.canModify()).isTrue();
        assertThat(detail.authorNickname()).isEqualTo("닉네임");
        assertThat(detail.images()).hasSize(1);
        assertThat(detail.images().get(0).originalFileName()).isEqualTo("a.jpg");
        assertThat(detail.comments().getContent()).hasSize(1);
        assertThat(detail.comments().getContent().get(0).deletable()).isFalse();
    }

    @Test
    @DisplayName("별점순 정렬 검색은 별점과 작성일 역순 페이지로 조회한다")
    void givenRatingSort_whenSearch_thenPageableSortedByRatingDesc() {
        Member author = member(1L);
        Review existing = review(9L, author);
        Page<Review> page = new PageImpl<>(List.of(existing),
                PageRequest.of(0, 10,
                        Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"))), 1);
        given(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(page);
        given(commentRepository.countByReviewIds(anyList())).willReturn(List.of());
        given(reviewImageRepository.findFirstImageKeyByReviewIds(anyList()))
                .willReturn(List.of());

        Page<SummaryView> result = reviewService().search(null, "강남구", 4, "rating", 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        then(reviewRepository).should().findAll(any(Specification.class), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort())
                .containsExactly(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).authorNickname()).isEqualTo("닉네임");
        assertThat(result.getContent().get(0).commentCount()).isZero();
    }

    @Test
    @DisplayName("키워드가 최소 길이 이상이면 키워드 조건을 담은 필터로 조회한다")
    void givenKeyword_whenSearch_thenFilterContainsKeywordPredicate() {
        given(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        reviewService().search("성수동", "강남구", null, "latest", 0);

        ArgumentCaptor<Specification<Review>> captor = captorForSpecification();
        then(reviewRepository).should().findAll(captor.capture(), any(Pageable.class));

        Specification<Review> spec = captor.getValue();
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<Review> root = mock(Root.class);
        stubCriteria(root, cb);
        stubLower(cb);
        spec.toPredicate(root, mock(CriteriaQuery.class), cb);

        then(cb).should(times(3)).like(any(Expression.class), eq("%성수동%"));
        then(cb).should().equal(any(Expression.class), eq("강남구"));
        then(cb).should().greaterThanOrEqualTo(any(Expression.class), eq(1));
    }

    @Test
    @DisplayName("키워드가 최소 길이보다 짧으면 키워드 조건 없이 조회한다")
    void givenShortKeyword_whenSearch_thenNoKeywordPredicate() {
        given(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        reviewService().search("성", "강남구", 4, "latest", 0);

        ArgumentCaptor<Specification<Review>> captor = captorForSpecification();
        then(reviewRepository).should().findAll(captor.capture(), any(Pageable.class));

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<Review> root = mock(Root.class);
        stubCriteria(root, cb);
        captor.getValue().toPredicate(root, mock(CriteriaQuery.class), cb);

        then(cb).should(never()).like(any(Expression.class), anyString());
        then(cb).should().equal(any(Expression.class), eq("강남구"));
    }

    @Test
    @DisplayName("최소 별점이 허용 범위를 넘으면 상한으로 클램프해 조회한다")
    void givenTooHighMinRating_whenSearch_thenClampedToMax() {
        given(reviewRepository.findAll(any(Specification.class), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 10), 0));

        reviewService().search(null, null, 99, "latest", 0);

        ArgumentCaptor<Specification<Review>> captor = captorForSpecification();
        then(reviewRepository).should().findAll(captor.capture(), any(Pageable.class));

        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        Root<Review> root = mock(Root.class);
        stubCriteria(root, cb);
        captor.getValue().toPredicate(root, mock(CriteriaQuery.class), cb);

        then(cb).should().greaterThanOrEqualTo(any(Expression.class), eq(5));
        then(cb).should(never()).like(any(Expression.class), anyString());
    }
}