---
name: spring-boot4
description: TasteReview의 최신 Spring 스택 문법을 판단하는 단일 출처다. Spring Boot 4, Spring Security 7, Spring Data JPA 4, Hibernate 7, Testcontainers 2, Docker Compose v2 관련 질문이면 반드시 이 스킬을 사용한다. "이 문법 맞나요", "요즘 스타터 이름", "Security 설정 어떻게", "JPA 문법", "의존성 좌표", "Dockerfile", "compose 작성", "마이그레이션", "Boot 3과 뭐가 다른지" 같은 질문과 코드·설계 리뷰에서 최신 문법 준수 여부를 확인할 때도 로드한다. 오래된 블로그의 옛 문법(.and(), antMatchers, spring-boot-starter-web, @MockBean, jpamodelgen, old Testcontainers 좌표)을 참고하려 할 때도 이 스킬로 사실 확인을 먼저 한다.
---

# Spring Boot 4 최신 문법 스킬

TasteReview는 직접 코드를 쓰지 않지만, 판단 기준은 최신 문법이어야 한다(루트 `AGENTS.md` 참고).
예전 버전에서 옳았던 문법이 Boot 4에서는 틀리거나 동작이 조용히 바뀐 경우가 많으므로, 설계·리뷰 전에 이 스킬로 현재 문법을 고정한다.

## 단일 출처

Spring 스택 문법의 사실은 `references/latest-syntax.md`에만 모아 둔다.
본문은 판단 순서와 핵심 규칙이고, 정확한 좌표·코드·마이그레이션 표는 그 파일을 읽어 확인한다.

## 판단 순서

1. **기본값은 이 스킬을 따른다.** 아래 핵심 규칙과 `references/latest-syntax.md`의 표를 먼저 적용한다.
2. **스킬이 답하지 않으면 공식 출처를 검색해 검증한다.** 대상은 `spring.io`, `docs.spring.io` 참고 문서·릴리스 노트, `spring-projects` GitHub 릴리스 정보다. 최신 연도(2026) 자료를 우선한다.
3. **추측과 옛 기억을 단정으로 쓰지 않는다.** 근거 없으면 "가정"으로 표시하고 결정이 갈리는 것만 사용자에게 묻는다.
4. **리뷰에서는 어긋난 문법을 구체적으로 짝지어 지적한다.** 어떤 파일의 어떤 표현이 어떤 Boot 4 문법과 충돌하는지와 교체안을 함께 제시한다.

## 핵심 규칙 (상세는 references)

- **플랫폼 기준.** Boot 4는 Spring Framework 7, Jakarta EE 11, Jackson 3, Servlet 6.1, Java 17 이상을 요구한다. Java 21 요구는 오해다.
- **모듈형 스타터.** `spring-boot-starter-web`은 `spring-boot-starter-webmvc`로 이름이 바뀌었다. 테스트 기능은 전용 스타터(`webmvc-test`, `data-jpa-test`, `security-test`, `thymeleaf-test`)를 명시해야 하며, `spring-boot-starter-test`만으로는 MockMvc·WebTestClient가 주입되지 않는다.
- **보안(Security 7).** 람다 DSL이 강제다. `.and()` 연쇄, `authorizeRequests`, `antMatchers`/`mvcMatchers`는 제거되었다. 규칙은 `requestMatchers` + `PathPatternRequestMatcher` 기준이며, 패턴 중간 `**`은 금지다. 풀 컨텍스트 테스트에서 MockMvc는 `@AutoConfigureMockMvc`를 붙여야 주입된다.
- **JPA(Hibernate 7 / Data JPA 4).** 정적 메타모델 생성기는 `hibernate-processor`로 바뀌었다. 파생 쿼리는 Criteria 대신 JPQL 문자열로 생성된다. null성은 JSpecify `@NullMarked` 체계로 선언하며, record는 값 객체·프로젝션에만 쓰고 엔티티가 될 수 없다.
- **Testcontainers 2.** 모듈 좌표가 `testcontainers-` 접두사를 달고 패키지도 이동했다. `@ServiceConnection`은 `spring-boot-testcontainers`로 불러오며, 서비스 커넥션 팩토리는 SQL/JDBC 스타터가 제공하므로 JDBC·JPA를 쓴다면 해당 스타터도 있어야 한다.
- **Docker Compose v2.** 파일은 `compose.yml`, 명령은 `docker compose`(하이픈 아님). `depends_on`은 `condition: service_healthy`와 `healthcheck`로 준비 상태를 기다리고, 비루트 `USER`·named volume·secrets가 기본 기대치다.

## 리뷰 기준

정확성과 최신 문법 준수를 기준으로 지적하되, 문법이 아니라 취향·구조의 문제는 그렇다고 밝히고 사용자 판단에 맡긴다.
문법 판단의 근거는 `references/latest-syntax.md`의 표 항목을 인용해 제시한다.