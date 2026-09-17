package org.example.tastereview.application.review;

import lombok.RequiredArgsConstructor;
import org.example.tastereview.application.AppProperties;
import org.example.tastereview.domain.exception.ForbiddenOperationException;
import org.example.tastereview.domain.exception.MemberNotFoundException;
import org.example.tastereview.domain.exception.ReviewNotFoundException;
import org.example.tastereview.domain.member.Member;
import org.example.tastereview.domain.repository.CommentRepository;
import org.example.tastereview.domain.repository.MemberRepository;
import org.example.tastereview.domain.repository.ReviewImageRepository;
import org.example.tastereview.domain.repository.ReviewRepository;
import org.example.tastereview.domain.review.Review;
import org.example.tastereview.domain.review.ReviewImage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final CommentRepository commentRepository;
    private final MemberRepository memberRepository;
    private final AppProperties appProperties;
    private final ImageValidator imageValidator;

    @Transactional
    public Review create(long memberId, ReviewCommand cmd) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(MemberNotFoundException::new);
        validateRegionAndRating(cmd);
        imageValidator.validate(cmd.getImages());
        Review review = new Review(
                cmd.getStoreName(), cmd.getRegion(), cmd.getRating(),
                cmd.getTitle(), cmd.getContent(), member);
        addImages(review, cmd.getImages());
        return reviewRepository.save(review);
    }

    @Transactional
    public Review update(long memberId, long reviewId, ReviewCommand cmd) {
        Review review = reviewRepository.findDetailById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);
        if (!review.getMember().getId().equals(memberId)) {
            throw new ForbiddenOperationException();
        }
        validateRegionAndRating(cmd);
        imageValidator.validate(cmd.getImages());
        review.update(cmd.getStoreName(), cmd.getRegion(), cmd.getRating(),
                cmd.getTitle(), cmd.getContent());
        replaceImages(review, cmd.getImages());
        return reviewRepository.save(review);
    }

    @Transactional
    public void delete(long memberId, long reviewId) {
        Review review = reviewRepository.findDetailById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);
        if (!review.getMember().getId().equals(memberId)) {
            throw new ForbiddenOperationException();
        }
        commentRepository.deleteAllByReviewId(reviewId);
        reviewRepository.delete(review);
    }

    public DetailView getDetail(long reviewId, long viewerId, int commentPage) {
        Review review = reviewRepository.findDetailById(reviewId)
                .orElseThrow(ReviewNotFoundException::new);
        List<DetailView.ImageView> images = reviewImageRepository
                .findByReviewIdOrderByDisplayOrderAsc(reviewId)
                .stream()
                .map(image -> new DetailView.ImageView(
                        image.getId(), image.getMimeType(), image.getOriginalFileName(),
                        image.getDisplayOrder()))
                .toList();
        Pageable commentPageable = PageRequest.of(
                commentPage, appProperties.getCommentPageSize(),
                Sort.by("createdAt").ascending());
        Page<DetailView.CommentView> comments =
                commentRepository.findPageByReviewId(reviewId, commentPageable)
                        .map(comment -> new DetailView.CommentView(
                                comment.getId(), comment.getContent(),
                                comment.getMember().getNickname(), comment.getCreatedAt(),
                                comment.getMember().getId().equals(viewerId)));
        boolean canModify = review.getMember().getId().equals(viewerId);
        return new DetailView(
                review.getId(), review.getStoreName(), review.getRegion(), review.getRating(),
                review.getTitle(), review.getContent(), review.getMember().getId(),
                review.getMember().getNickname(), review.getCreatedAt(), review.getUpdatedAt(),
                canModify, images, comments);
    }

    public Page<SummaryView> search(String keyword, String region, Integer minRating,
                                    String sort, int page) {
        String normalizedKeyword = normalizeKeyword(keyword);
        Integer normalizedRating = normalizeRating(minRating);
        Pageable pageable = buildPageable(sort, page);
        Page<Review> reviews = reviewRepository.findAll(
                ReviewSpecifications.filter(normalizedKeyword, region, normalizedRating),
                pageable);
        List<Long> reviewIds = reviews.getContent().stream().map(Review::getId).toList();
        Map<Long, Long> commentCounts = new HashMap<>();
        Map<Long, Long> representativeImageIds = new HashMap<>();
        collectCommentCounts(reviewIds, commentCounts);
        collectRepresentativeImages(reviewIds, representativeImageIds);
        return reviews.map(review -> new SummaryView(
                review.getId(), review.getStoreName(), review.getRegion(), review.getRating(),
                review.getTitle(), review.getMember().getNickname(), review.getCreatedAt(),
                commentCounts.getOrDefault(review.getId(), 0L),
                representativeImageIds.get(review.getId())));
    }

    private void collectCommentCounts(List<Long> reviewIds, Map<Long, Long> commentCounts) {
        if (reviewIds.isEmpty()) {
            return;
        }
        for (Object[] row : commentRepository.countByReviewIds(reviewIds)) {
            commentCounts.put((Long) row[0], (Long) row[1]);
        }
    }

    private void collectRepresentativeImages(List<Long> reviewIds,
                                             Map<Long, Long> representativeImageIds) {
        if (reviewIds.isEmpty()) {
            return;
        }
        for (Object[] row : reviewImageRepository.findFirstImageKeyByReviewIds(reviewIds)) {
            representativeImageIds.putIfAbsent((Long) row[0], (Long) row[1]);
        }
    }

    private void validateRegionAndRating(ReviewCommand cmd) {
        boolean regionValid = cmd.getRegion() != null
                && appProperties.getRegions().contains(cmd.getRegion());
        Integer rating = cmd.getRating();
        boolean ratingValid = rating != null
                && rating >= appProperties.getReviewRatingMin()
                && rating <= appProperties.getReviewRatingMax();
        if (!regionValid || !ratingValid) {
            throw new IllegalArgumentException("지역 또는 별점 값이 올바르지 않습니다");
        }
    }

    private void replaceImages(Review review, List<MultipartFile> files) {
        List<MultipartFile> validFiles = validFiles(files);
        if (validFiles.isEmpty()) {
            return;
        }
        review.getImages().clear();
        addImages(review, validFiles);
    }

    private void addImages(Review review, List<MultipartFile> files) {
        int displayOrder = 0;
        for (MultipartFile file : validFiles(files)) {
            review.addImage(toImage(review, file, displayOrder));
            displayOrder++;
        }
    }

    private ReviewImage toImage(Review review, MultipartFile file, int displayOrder) {
        try {
            return new ReviewImage(
                    review, file.getContentType(), file.getOriginalFilename(),
                    displayOrder, file.getBytes());
        } catch (IOException ex) {
            throw new IllegalStateException("이미지 데이터를 읽을 수 없습니다", ex);
        }
    }

    private List<MultipartFile> validFiles(List<MultipartFile> files) {
        if (files == null) {
            return List.of();
        }
        return files.stream()
                .filter(file -> file != null && !file.isEmpty() && file.getSize() > 0)
                .toList();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null) {
            return null;
        }
        String trimmed = keyword.trim();
        if (trimmed.length() < appProperties.getSearchMinKeywordLength()) {
            return null;
        }
        return trimmed;
    }

    private Integer normalizeRating(Integer minRating) {
        Integer rating = minRating == null ? appProperties.getReviewRatingMin() : minRating;
        if (rating > appProperties.getReviewRatingMax()) {
            return appProperties.getReviewRatingMax();
        }
        return rating;
    }

    private Pageable buildPageable(String sort, int page) {
        if ("rating".equals(sort)) {
            return PageRequest.of(page, appProperties.getReviewPageSize(),
                    Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt")));
        }
        return PageRequest.of(page, appProperties.getReviewPageSize(),
                Sort.by(Sort.Order.desc("createdAt")));
    }
}