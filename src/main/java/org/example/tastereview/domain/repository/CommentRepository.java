package org.example.tastereview.domain.repository;

import org.example.tastereview.domain.comment.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    @Query("select c.review.id, count(c) from Comment c " +
            "where c.review.id in :reviewIds group by c.review.id")
    List<Object[]> countByReviewIds(@Param("reviewIds") Collection<Long> reviewIds);

    @Query("select c from Comment c join fetch c.member " +
            "where c.review.id = :reviewId order by c.createdAt asc")
    Page<Comment> findPageByReviewId(@Param("reviewId") long reviewId, Pageable pageable);

    @Modifying
    @Query("delete from Comment c where c.review.id = :reviewId")
    void deleteAllByReviewId(@Param("reviewId") long reviewId);
}