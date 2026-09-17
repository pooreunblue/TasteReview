package org.example.tastereview.domain.review;

import org.example.tastereview.domain.comment.Comment;
import org.example.tastereview.domain.member.Member;
import org.example.tastereview.domain.repository.CommentRepository;
import org.example.tastereview.domain.repository.MemberRepository;
import org.example.tastereview.domain.repository.ReviewImageRepository;
import org.example.tastereview.domain.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class ReviewImageRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @Autowired
    private ReviewImageRepository reviewImageRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    @DisplayName("리뷰별 대표 사진 후보는 노출 순서 오름차순 정렬로 반환한다")
    void givenImages_whenFindFirstImageKey_thenLowestDisplayOrderReturned() {
        Member member = memberRepository.save(new Member("a@test.com", "hash", "테스터"));
        Review review = reviewRepository.save(
                new Review("맛집", "강남구", 5, "제목", "내용", member));
        ReviewImage orderTwo =
                new ReviewImage(review, "image/jpeg", "c.jpg", 2, new byte[]{3});
        ReviewImage orderZero =
                new ReviewImage(review, "image/jpeg", "a.jpg", 0, new byte[]{1});
        ReviewImage orderOne =
                new ReviewImage(review, "image/jpeg", "b.jpg", 1, new byte[]{2});
        reviewImageRepository.saveAll(List.of(orderTwo, orderZero, orderOne));

        List<Object[]> rows = reviewImageRepository
                .findFirstImageKeyByReviewIds(List.of(review.getId()));

        assertThat(rows).hasSize(3);
        assertThat(rows.get(0)[0]).isEqualTo(review.getId());
        assertThat(rows.get(0)[1]).isEqualTo(orderZero.getId());
    }

    @Test
    @DisplayName("여러 리뷰를 조회하면 리뷰별 첫 사진 후보를 각각 반환한다")
    void givenTwoReviews_whenFindFirstImageKey_thenEachRepresentativeReturned() {
        Member member = memberRepository.save(new Member("a@test.com", "hash", "테스터"));
        Review first = reviewRepository.save(
                new Review("맛집1", "강남구", 5, "제목", "내용", member));
        Review second = reviewRepository.save(
                new Review("맛집2", "서초구", 4, "제목", "내용", member));
        ReviewImage firstZero =
                new ReviewImage(first, "image/jpeg", "a.jpg", 0, new byte[]{1});
        ReviewImage firstOne =
                new ReviewImage(first, "image/jpeg", "b.jpg", 1, new byte[]{2});
        ReviewImage secondOne =
                new ReviewImage(second, "image/jpeg", "c.jpg", 1, new byte[]{3});
        ReviewImage secondZero =
                new ReviewImage(second, "image/jpeg", "d.jpg", 0, new byte[]{4});
        reviewImageRepository.saveAll(List.of(firstZero, firstOne, secondOne, secondZero));

        List<Object[]> rows = reviewImageRepository
                .findFirstImageKeyByReviewIds(List.of(first.getId(), second.getId()));

        Map<Long, Long> representative = rows.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1], (a, b) -> a));
        assertThat(representative)
                .containsEntry(first.getId(), firstZero.getId())
                .containsEntry(second.getId(), secondZero.getId());
    }

    @Test
    @DisplayName("리뷰별 댓글 수를 그룹으로 집계한다")
    void givenComments_whenCountByReviewIds_thenGroupedCounts() {
        Member member = memberRepository.save(new Member("a@test.com", "hash", "테스터"));
        Review withTwo = reviewRepository.save(
                new Review("맛집1", "강남구", 5, "제목", "내용", member));
        Review withOne = reviewRepository.save(
                new Review("맛집2", "서초구", 4, "제목", "내용", member));
        commentRepository.saveAll(List.of(
                new Comment(member, withTwo, "댓글1"),
                new Comment(member, withTwo, "댓글2"),
                new Comment(member, withOne, "댓글3")));

        List<Object[]> rows = commentRepository
                .countByReviewIds(List.of(withTwo.getId(), withOne.getId()));

        Map<Long, Long> counts = rows.stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1]));
        assertThat(counts)
                .containsEntry(withTwo.getId(), 2L)
                .containsEntry(withOne.getId(), 1L);
    }

    @Test
    @DisplayName("리뷰 사진 조회는 노출 순서 오름차순으로 반환한다")
    void givenImages_whenFindByReviewId_thenOrderedByDisplayOrder() {
        Member member = memberRepository.save(new Member("a@test.com", "hash", "테스터"));
        Review review = reviewRepository.save(
                new Review("맛집", "강남구", 5, "제목", "내용", member));
        ReviewImage orderTwo =
                new ReviewImage(review, "image/jpeg", "c.jpg", 2, new byte[]{3});
        ReviewImage orderZero =
                new ReviewImage(review, "image/jpeg", "a.jpg", 0, new byte[]{1});
        ReviewImage orderOne =
                new ReviewImage(review, "image/jpeg", "b.jpg", 1, new byte[]{2});
        reviewImageRepository.saveAll(List.of(orderTwo, orderZero, orderOne));

        List<ReviewImage> images =
                reviewImageRepository.findByReviewIdOrderByDisplayOrderAsc(review.getId());

        assertThat(images).extracting(ReviewImage::getDisplayOrder).containsExactly(0, 1, 2);
    }
}