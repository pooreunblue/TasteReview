package org.example.tastereview.application.comment;

import org.example.tastereview.domain.comment.Comment;
import org.example.tastereview.domain.exception.ForbiddenOperationException;
import org.example.tastereview.domain.exception.ReviewNotFoundException;
import org.example.tastereview.domain.member.Member;
import org.example.tastereview.domain.repository.CommentRepository;
import org.example.tastereview.domain.repository.MemberRepository;
import org.example.tastereview.domain.repository.ReviewRepository;
import org.example.tastereview.domain.review.Review;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private MemberRepository memberRepository;

    private CommentService commentService() {
        return new CommentService(commentRepository, reviewRepository, memberRepository);
    }

    private Member member(long id) {
        Member member = new Member("a@b.com", "hash", "닉네임");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Review review(long id, Member author) {
        return new Review("맛집", "강남구", 5, "제목", "맛있는 가게입니다", author);
    }

    @Test
    @DisplayName("리뷰와 회원이 존재하면 공백을 제거한 댓글을 저장한다")
    void givenReviewAndMember_whenAdd_thenCommentSaved() {
        Member author = member(1L);
        Review review = review(9L, author);
        Member commenter = member(2L);
        given(reviewRepository.findDetailById(9L)).willReturn(Optional.of(review));
        given(memberRepository.findById(2L)).willReturn(Optional.of(commenter));
        given(commentRepository.save(any(Comment.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        Comment saved = commentService().add(2L, 9L, "  맛있어요  ");

        assertThat(saved.getContent()).isEqualTo("맛있어요");
        assertThat(saved.getMember()).isSameAs(commenter);
        assertThat(saved.getReview()).isSameAs(review);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("리뷰가 없으면 댓글을 저장하지 않는다")
    void givenNoReview_whenAdd_thenReviewNotFound() {
        given(reviewRepository.findDetailById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> commentService().add(2L, 9L, "내용"))
                .isInstanceOf(ReviewNotFoundException.class);
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글이 공백이면 저장하지 않는다")
    void givenBlankContent_whenAdd_thenIllegalArgumentException() {
        Member author = member(1L);
        given(reviewRepository.findDetailById(9L))
                .willReturn(Optional.of(review(9L, author)));

        assertThatThrownBy(() -> commentService().add(2L, 9L, "   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글은 1자 이상 500자 이하여야 합니다");
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글이 500자를 넘으면 저장하지 않는다")
    void givenTooLongContent_whenAdd_thenIllegalArgumentException() {
        Member author = member(1L);
        given(reviewRepository.findDetailById(9L))
                .willReturn(Optional.of(review(9L, author)));

        assertThatThrownBy(() -> commentService().add(2L, 9L, "내".repeat(501)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글은 1자 이상 500자 이하여야 합니다");
        verify(commentRepository, never()).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 작성자 본인이면 댓글을 삭제한다")
    void givenOwner_whenDelete_thenCommentDeleted() {
        Member commenter = member(2L);
        Review review = review(9L, commenter);
        Comment comment = new Comment(commenter, review, "내용");
        ReflectionTestUtils.setField(comment, "id", 3L);
        given(commentRepository.findById(3L)).willReturn(Optional.of(comment));

        commentService().delete(2L, 3L);

        verify(commentRepository).delete(comment);
    }

    @Test
    @DisplayName("타인이 댓글을 삭제하면 금지 예외가 발생한다")
    void givenNotOwner_whenDelete_thenForbiddenOperationException() {
        Member commenter = member(2L);
        Comment comment = new Comment(commenter, review(9L, commenter), "내용");
        ReflectionTestUtils.setField(comment, "id", 3L);
        given(commentRepository.findById(3L)).willReturn(Optional.of(comment));

        assertThatThrownBy(() -> commentService().delete(99L, 3L))
                .isInstanceOf(ForbiddenOperationException.class);
        verify(commentRepository, never()).delete(any(Comment.class));
    }

    @Test
    @DisplayName("이미 삭제된 댓글이면 오류 없이 무시한다")
    void givenMissingComment_whenDelete_thenSilentlyIgnored() {
        given(commentRepository.findById(3L)).willReturn(Optional.empty());

        assertThatCode(() -> commentService().delete(2L, 3L))
                .doesNotThrowAnyException();
        verify(commentRepository, never()).delete(any(Comment.class));
    }
}