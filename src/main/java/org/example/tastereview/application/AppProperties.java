package org.example.tastereview.application;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private List<String> regions = List.of(
            "강남구", "강동구", "강북구", "강서구", "관악구", "광진구", "구로구", "금천구",
            "노원구", "도봉구", "동대문구", "동작구", "마포구", "서대문구", "서초구", "성동구",
            "성북구", "송파구", "양천구", "영등포구", "용산구", "은평구", "종로구", "중구",
            "중랑구");

    private int reviewRatingMin = 1;
    private int reviewRatingMax = 5;
    private int reviewPageSize = 10;
    private int commentPageSize = 20;
    private int searchMinKeywordLength = 2;
    private int imageMaxCount = 5;

    private List<String> imageAllowedTypes = List.of("image/jpeg", "image/png", "image/webp");
}