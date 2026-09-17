package org.example.tastereview.config;

import java.util.ArrayList;
import java.util.List;
import org.springframework.core.env.Environment;

public final class EnvGuard {

    private static final List<String> REQUIRED_ENV_VARS = List.of(
        "SPRING_DATASOURCE_URL",
        "SPRING_DATASOURCE_USERNAME",
        "SPRING_DATASOURCE_PASSWORD"
    );

    private EnvGuard() {
    }

    public static List<String> missing(Environment environment) {
        List<String> missing = new ArrayList<>();
        for (String envVar : REQUIRED_ENV_VARS) {
            String value = environment.getProperty(envVar, "");
            if (value.isBlank()) {
                missing.add(envVar);
            }
        }
        return missing;
    }
}