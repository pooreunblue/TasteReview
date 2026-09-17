package org.example.tastereview.application.comment;

import lombok.RequiredArgsConstructor;
import org.example.tastereview.domain.comment.Comment;
import org.example.tastereview.domain.exception.ForbiddenOperationException;
import org.example.tastereview.domain.exception.MemberNotFoundException;
import org.example.tastereview.domain.exception.ReviewNotFoundException;
import org.example.tastereview.domain.member.Member;
import org.example.tastereview.domain.repository.CommentRepository;
import org.example.tastereview.domain.repository.MemberRepository;
import org.example.tastereview.domain.repository.ReviewRepository;
import org.example.tastereview.domain.review.Review;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CommentService {

    private static final int MAX_CONTENT_LENGTH = 500;

    private final CommentRepository commentRepository;
    private final ReviewRepository reviewRepository;
    private final MemberRepository memberRepository;

    public Comment add(long memberId, long reviewId, String content) {
        Review review = reviewRepository.findDetailById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);
        String trimmedContent = content == null ? "" : content.trim();
        if (trimmedContent.isEmpty() || trimmedContent.length() > MAX_CONTENT_LENGTH) {
            throw new IllegalArgumentException("댓글은 1자 이상 500자 이하여야 합니다");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        return commentRepository.save(new Comment(member, review, trimmedContent));
    }

    public void delete(long memberId, long commentId) {
        commentRepository.findById(commentId).ifPresent(comment -> {
            if (!comment.getMember().getId().equals(memberId)) {
                throw new ForbiddenOperationException();
            }
            commentRepository.delete(comment);
        });
    }
}