package org.example.tastereview.application.review;

import java.time.LocalDateTime;

public record SummaryView(
        long id,
        String storeName,
        String region,
        int rating,
        String title,
        String authorNickname,
        LocalDateTime createdAt,
        long commentCount,
        Long representativeImageId
) {
}