package org.example.tastereview.application.review;

import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.example.tastereview.domain.review.Review;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ReviewSpecifications {

    private ReviewSpecifications() {
    }

    public static Specification<Review> filter(String keyword, String region, Integer minRating) {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("member", JoinType.LEFT);
            }
            List<Predicate> predicates = new ArrayList<>();

            String normalizedKeyword = keyword == null ? "" : keyword.trim();
            if (!normalizedKeyword.isBlank()) {
                String likePattern = "%" + normalizedKeyword.toLowerCase(Locale.ROOT) + "%";
                Predicate storeNameMatch = cb.like(cb.lower(root.get("storeName")), likePattern);
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), likePattern);
                Predicate contentMatch = cb.like(cb.lower(root.get("content")), likePattern);
                predicates.add(cb.or(storeNameMatch, titleMatch, contentMatch));
            }

            if (region != null && !region.isBlank()) {
                predicates.add(cb.equal(root.get("region"), region.trim()));
            }

            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}