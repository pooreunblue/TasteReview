package org.example.tastereview.application.review;

import org.example.tastereview.application.AppProperties;
import org.example.tastereview.domain.exception.ImageValidationException;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Component
public class ImageValidator {

    private static final String MAX_FILE_SIZE_PROPERTY = "spring.servlet.multipart.max-file-size";
    private static final DataSize DEFAULT_MAX_FILE_SIZE = DataSize.ofMegabytes(5);

    private final AppProperties appProperties;
    private final Environment environment;

    public ImageValidator(AppProperties appProperties, Environment environment) {
        this.appProperties = appProperties;
        this.environment = environment;
    }

    public void validate(List<MultipartFile> files) {
        if (files == null) {
            return;
        }
        List<MultipartFile> validFiles = validFiles(files);
        List<String> messages = new ArrayList<>();
        if (validFiles.size() > appProperties.getImageMaxCount()) {
            messages.add(appProperties.getImageMaxCount() + "장 이하의 사진만 등록할 수 있습니다");
        }
        long maxFileSizeBytes = resolveMaxFileSizeBytes();
        for (MultipartFile file : validFiles) {
            String contentType = String.valueOf(file.getContentType());
            if (!appProperties.getImageAllowedTypes().contains(contentType)) {
                messages.add(file.getOriginalFilename() + "은(는) 지원하지 않는 이미지 형식입니다");
            }
            if (file.getSize() > maxFileSizeBytes) {
                int maxMb = (int) (maxFileSizeBytes / (1024 * 1024));
                messages.add(file.getOriginalFilename()
                        + " 파일이 너무 큽니다. (최대 " + maxMb + "MB)");
            }
        }
        if (!messages.isEmpty()) {
            throw new ImageValidationException(messages);
        }
    }

    private List<MultipartFile> validFiles(List<MultipartFile> files) {
        return files.stream()
                .filter(file -> file != null && !file.isEmpty() && file.getSize() > 0)
                .toList();
    }

    private long resolveMaxFileSizeBytes() {
        String value = environment.getProperty(MAX_FILE_SIZE_PROPERTY, "5MB");
        try {
            return DataSize.parse(value).toBytes();
        } catch (IllegalArgumentException ex) {
            return DEFAULT_MAX_FILE_SIZE.toBytes();
        }
    }
}