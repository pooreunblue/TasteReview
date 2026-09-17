package org.example.tastereview.domain.review;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "review_images", indexes = {
        @Index(name = "idx_review_image_review", columnList = "review_id")
})
public class ReviewImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id")
    private Review review;

    @Column(nullable = false, length = 50)
    private String mimeType;

    @Column(nullable = false, length = 200)
    private String originalFileName;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] data;

    public ReviewImage(Review review, String mimeType, String originalFileName,
                       int displayOrder, byte[] data) {
        this.review = review;
        this.mimeType = mimeType;
        this.originalFileName = originalFileName;
        this.displayOrder = displayOrder;
        this.data = data;
    }
}