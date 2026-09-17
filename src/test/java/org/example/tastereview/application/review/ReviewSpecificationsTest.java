package org.example.tastereview.application.review;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.example.tastereview.domain.review.Review;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.domain.Specification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

class ReviewSpecificationsTest {

    @SuppressWarnings("unchecked")
    private void stubCriteria(Root<Review> root, CriteriaBuilder cb) {
        Path<Object> path = mock(Path.class);
        given(root.get(anyString())).willReturn(path);
        Expression<String> lower = mock(Expression.class);
        given(cb.lower(any(Expression.class))).willReturn(lower);
    }

    @Test
    @DisplayName("모든 조건이 있으면 키워드 OR 지역 AND 최소 별점 조건을 만든다")
    void givenAllConditions_whenApply_thenPredicatesCombined() {
        Specification<Review> filter = ReviewSpecifications.filter("성수", "강남구", 4);

        Root<Review> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);
        stubCriteria(root, cb);

        filter.toPredicate(root, query, cb);

        then(cb).should(times(3)).like(any(Expression.class), eq("%성수%"));
        then(cb).should().or(any(), any(), any());
        then(cb).should().equal(any(Expression.class), eq("강남구"));
        then(cb).should().greaterThanOrEqualTo(any(Expression.class), eq(4));
        then(cb).should().and(any(Predicate[].class));
    }

    @Test
    @DisplayName("키워드가 null이면 키워드 조건을 만들지 않는다")
    void givenNullKeyword_whenApply_thenNoKeywordPredicate() {
        Specification<Review> filter = ReviewSpecifications.filter(null, null, null);

        Root<Review> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        filter.toPredicate(root, query, cb);

        then(cb).should(never()).like(any(Expression.class), anyString());
        then(cb).should(never()).or(any(), any(), any());
        then(cb).should().and(any(Predicate[].class));
    }

    @Test
    @DisplayName("공백 키워드와 공백 지역은 검색 조건을 만들지 않는다")
    void givenBlankKeywordAndRegion_whenApply_thenNoSearchConditions() {
        Specification<Review> filter = ReviewSpecifications.filter("   ", "   ", null);

        Root<Review> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class);

        filter.toPredicate(root, query, cb);

        then(cb).should(never()).like(any(Expression.class), anyString());
        then(cb).should(never()).equal(any(Expression.class), anyString());
    }
}