package org.example.tastereview.web;

import org.example.tastereview.domain.exception.ReviewNotFoundException;
import org.example.tastereview.domain.review.ReviewImage;
import org.example.tastereview.domain.repository.ReviewImageRepository;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class ImageController {

    private final ReviewImageRepository reviewImageRepository;

    public ImageController(ReviewImageRepository reviewImageRepository) {
        this.reviewImageRepository = reviewImageRepository;
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<Resource> image(@PathVariable long id) {
        ReviewImage image = reviewImageRepository.findById(id)
                .orElseThrow(ReviewNotFoundException::new);
        ByteArrayResource resource = new ByteArrayResource(image.getData());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.getMimeType()))
                .contentLength(resource.contentLength())
                .body(resource);
    }
}