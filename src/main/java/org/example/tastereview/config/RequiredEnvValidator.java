package org.example.tastereview.config;

import java.util.List;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("prod")
public class RequiredEnvValidator {

    @Bean
    public static BeanFactoryPostProcessor requiredEnvGuard(Environment environment) {
        return beanFactory -> {
            List<String> missing = EnvGuard.missing(environment);
            if (!missing.isEmpty()) {
                throw new IllegalStateException(
                    "필수 환경변수가 설정되지 않았습니다: " + missing);
            }
        };
    }
}
