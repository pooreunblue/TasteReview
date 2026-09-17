package org.example.tastereview.application.review;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Getter
@Setter
public class ReviewCommand {

    @NotBlank
    @Size(max = 63)
    private String storeName;

    @NotBlank
    @Size(max = 63)
    private String region;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotBlank
    @Size(max = 4000)
    private String content;

    private List<MultipartFile> images;
}