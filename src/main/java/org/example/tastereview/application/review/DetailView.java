package org.example.tastereview.application.review;

import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.List;

public record DetailView(
        long id,
        String storeName,
        String region,
        int rating,
        String title,
        String content,
        long authorId,
        String authorNickname,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        boolean canModify,
        List<ImageView> images,
        Page<CommentView> comments
) {

    public record ImageView(long id, String mimeType, String originalFileName, int displayOrder) {
    }

    public record CommentView(
            long id,
            String content,
            String authorNickname,
            LocalDateTime createdAt,
            boolean deletable
    ) {
    }
}