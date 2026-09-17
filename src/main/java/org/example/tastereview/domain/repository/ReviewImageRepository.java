package org.example.tastereview.domain.repository;

import org.example.tastereview.domain.review.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    @Query("select i.review.id, i.id from ReviewImage i " +
            "where i.review.id in :reviewIds " +
            "order by i.review.id asc, i.displayOrder asc")
    List<Object[]> findFirstImageKeyByReviewIds(@Param("reviewIds") Collection<Long> reviewIds);

    List<ReviewImage> findByReviewIdOrderByDisplayOrderAsc(long reviewId);
}