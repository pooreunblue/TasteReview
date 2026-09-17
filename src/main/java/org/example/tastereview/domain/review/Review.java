package org.example.tastereview.domain.review;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.tastereview.domain.member.Member;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "reviews", indexes = {
        @Index(name = "idx_review_created_at", columnList = "created_at"),
        @Index(name = "idx_review_region_rating", columnList = "region,rating"),
        @Index(name = "idx_review_store_name", columnList = "store_name")
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 63)
    private String storeName;

    @Column(nullable = false, length = 63)
    private String region;

    @Column(nullable = false)
    private int rating;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<ReviewImage> images = new ArrayList<>();

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public Review(String storeName, String region, int rating, String title, String content,
                  Member member) {
        this.storeName = storeName;
        this.region = region;
        this.rating = rating;
        this.title = title;
        this.content = content;
        this.member = member;
    }

    public void update(String storeName, String region, int rating, String title, String content) {
        this.storeName = storeName;
        this.region = region;
        this.rating = rating;
        this.title = title;
        this.content = content;
    }

    public void addImage(ReviewImage image) {
        this.images.add(image);
    }

    public void removeImage(ReviewImage image) {
        this.images.remove(image);
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}