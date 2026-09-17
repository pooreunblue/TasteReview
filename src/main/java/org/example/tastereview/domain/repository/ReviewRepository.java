package org.example.tastereview.domain.repository;

import org.example.tastereview.domain.review.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>,
        JpaSpecificationExecutor<Review> {

    @Query("select r from Review r join fetch r.member where r.id = :id")
    Optional<Review> findDetailById(@Param("id") long id);
}