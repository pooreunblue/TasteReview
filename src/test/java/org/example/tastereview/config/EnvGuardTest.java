package org.example.tastereview.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class EnvGuardTest {

    @Test
    @DisplayName("필수 환경변수가 모두 설정되어 있으면 빈 누락 목록을 반환한다")
    void givenAllRequiredEnvVars_whenVerify_thenReturnsEmptyList() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("SPRING_DATASOURCE_URL", "jdbc:postgresql://db:5432/tastereview")
            .withProperty("SPRING_DATASOURCE_USERNAME", "tastereview")
            .withProperty("SPRING_DATASOURCE_PASSWORD", "secret");

        List<String> missing = EnvGuard.missing(environment);

        assertThat(missing).isEmpty();
    }

    @Test
    @DisplayName("일부 환경변수가 누락되면 누락된 변수의 환경변수 이름을 반환한다")
    void givenMissingEnvVars_whenVerify_thenReturnsTheirEnvNames() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("SPRING_DATASOURCE_USERNAME", "tastereview");

        List<String> missing = EnvGuard.missing(environment);

        assertThat(missing).containsExactly(
            "SPRING_DATASOURCE_URL",
            "SPRING_DATASOURCE_PASSWORD"
        );
    }

    @Test
    @DisplayName("환경변수 값이 공백이면 누락으로 판정한다")
    void givenBlankValues_whenVerify_thenTreatsAsMissing() {
        MockEnvironment environment = new MockEnvironment()
            .withProperty("SPRING_DATASOURCE_URL", " ")
            .withProperty("SPRING_DATASOURCE_USERNAME", "tastereview")
            .withProperty("SPRING_DATASOURCE_PASSWORD", "");

        List<String> missing = EnvGuard.missing(environment);

        assertThat(missing).containsExactly(
            "SPRING_DATASOURCE_URL",
            "SPRING_DATASOURCE_PASSWORD"
        );
    }
}