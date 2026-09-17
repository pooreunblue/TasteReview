package org.example.tastereview.application.review;

import org.example.tastereview.application.AppProperties;
import org.example.tastereview.domain.exception.ImageValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ImageValidatorTest {

    private static final String MAX_FILE_SIZE_PROPERTY = "spring.servlet.multipart.max-file-size";

    @Mock
    private Environment environment;

    private final AppProperties appProperties = new AppProperties();

    @BeforeEach
    void setUp() {
        given(environment.getProperty(MAX_FILE_SIZE_PROPERTY, "5MB")).willReturn("5MB");
    }

    private ImageValidator validator() {
        return new ImageValidator(appProperties, environment);
    }

    private List<MultipartFile> jpegFiles(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> (MultipartFile) new MockMultipartFile(
                        "images", "a" + i + ".jpg", "image/jpeg", new byte[]{1}))
                .toList();
    }

    @Test
    @DisplayName("사진이 5장을 초과하면 장수 제한 메시지를 남긴다")
    void givenTooManyImages_whenValidate_thenCountMessage() {
        assertThatThrownBy(() -> validator().validate(jpegFiles(6)))
                .isInstanceOf(ImageValidationException.class)
                .satisfies(ex -> assertThat(((ImageValidationException) ex).getMessages())
                        .containsExactly("5장 이하의 사진만 등록할 수 있습니다"));
    }

    @Test
    @DisplayName("지원하지 않는 형식은 파일명을 포함한 메시지를 남긴다")
    void givenUnsupportedType_whenValidate_thenTypeMessage() {
        MultipartFile gif = new MockMultipartFile(
                "images", "a.gif", "image/gif", new byte[]{1});

        assertThatThrownBy(() -> validator().validate(List.of(gif)))
                .isInstanceOf(ImageValidationException.class)
                .satisfies(ex -> assertThat(((ImageValidationException) ex).getMessages())
                        .containsExactly("a.gif은(는) 지원하지 않는 이미지 형식입니다"));
    }

    @Test
    @DisplayName("파일 크기가 5MB를 넘으면 크기 메시지를 남긴다")
    void givenTooLargeFile_whenValidate_thenSizeMessage() {
        MultipartFile big = new MockMultipartFile(
                "images", "big.jpg", "image/jpeg",
                new byte[5 * 1024 * 1024 + 1]);

        assertThatThrownBy(() -> validator().validate(List.of(big)))
                .isInstanceOf(ImageValidationException.class)
                .satisfies(ex -> assertThat(((ImageValidationException) ex).getMessages())
                        .containsExactly("big.jpg 파일이 너무 큽니다. (최대 5MB)"));
    }

    @Test
    @DisplayName("허용 형식과 크기 이내의 사진은 통과한다")
    void givenAllowedImages_whenValidate_thenNoException() {
        assertThatCode(() -> validator().validate(jpegFiles(5)))
                .doesNotThrowAnyException();
    }
}