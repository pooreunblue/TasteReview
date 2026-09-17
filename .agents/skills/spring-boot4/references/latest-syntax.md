# Spring Boot 4 최신 문법 참조

이 문서는 TasteReview가 참고하는 단일 문법 출처다. 판단 전에 표와 예제를 확인한다.
근거: Spring Boot 4.x 공식 문서·마이그레이션 가이드, Spring Security 7 릴리스 노트,
Spring Data JPA 4.0 릴리스 노트, Testcontainers 2.0 마이그레이션 가이드, Docker Compose 사양(2026년 현재).

## 1. 플랫폼 기준 (Boot 4.1)

| 항목 | 값 |
| --- | --- |
| Java | 17 이상 (Java 21 요구는 오해. 공식 시스템 요구사항은 17+) |
| Spring Framework | 7 |
| Jakarta EE | 11 (Servlet 6.1) |
| Jackson | 3 (`tools.jackson.*`. Boot 3의 `com.fasterxml.jackson` 2.x와 다른 계열) |
| Spring Security | 7.x |
| Spring Data JPA | 4.0 / Hibernate 7 |
| JPA 사양 | Jakarta Persistence 3.2 |

## 2. 모듈형 스타터 (Boot 4 핵심 변경)

모든 스타터가 특정 기술에 집중된 모듈로 분리되었다. 전환기는 `spring-boot-starter-classic` /
`spring-boot-starter-test-classic`을 쓸 수 있지만, 목표 상태는 포커스된 스타터다.

| Boot 3 (ci) | Boot 4 (신) |
| --- | --- |
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` |
| `spring-boot-starter-aop` | `spring-boot-starter-aspectj` |
| `spring-boot-starter-webflux` | 클라이언트 전용은 `spring-boot-starter-webclient` |
| `flyway-core` | `spring-boot-starter-flyway` (자동 구성 필요 시) |
| `spring-boot-starter-test` | 기능별로 나뉨: `-webmvc-test`, `-webmvc-test` 외 전용 스타터 |
| (없음) | `spring-boot-starter-webmvc-test`, `spring-boot-starter-security-test`, `spring-boot-starter-data-jpa-test`, `spring-boot-starter-jdbc-test`, `spring-boot-starter-thymeleaf-test`, `spring-boot-resttestclient`, `spring-boot-webtestclient`, `spring-boot-testcontainers` |

Gradle 예시 (현재 TasteReview build.gradle과 동일 패턴):

```groovy
implementation 'org.springframework.boot:spring-boot-starter-webmvc'
implementation 'org.springframework.boot:spring-boot-starter-thymeleaf'
testImplementation 'org.springframework.boot:spring-boot-starter-webmvc-test'
testImplementation 'org.springframework.boot:spring-boot-starter-thymeleaf-test'
testImplementation 'org.springframework.boot:spring-boot-testcontainers'
```

## 3. 테스트 (Boot 4 기준)

- `@SpringBootTest`만으로는 **MockMvc가 자동 주입되지 않는다.** 슬라이스든 풀 컨텍스트든
  MockMvc를 쓰려면 `@AutoConfigureMockMvc`를 명시해야 한다.
- `@AutoConfigureMockMvc` 패키지는 `org.springframework.boot.webmvc.test.autoconfigure`로 이동했다.
- `@WebMvcTest`, `@DataJpaTest`, `@AutoConfigureTestDatabase` 등도 Boot 4 패키지로 이동했다.
- 컨트롤러 협력 객체 목(mock)은 Framework 7 기준 `@MockitoBean`(구 `@MockBean`)을 쓴다.

```java
@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @MockitoBean
    ReviewService reviewService;

    @Autowired
    MockMvc mockMvc;

    // given/when/then 시나리오로 작성
}
```

- 풀 컨텍스트 통합 테스트는 `@SpringBootTest` + `@AutoConfigureMockMvc`.
- JUnit 5 + AssertJ + Mockito BDD가 기본 테스트 도구다.

## 4. Spring Security 7

- **람다 DSL이 유일한 방식.** `.and()` 연쇄, `authorizeRequests()`, `antMatchers`/`mvcMatchers`는 제거.
- 요청 매칭은 `PathPatternRequestMatcher` 하나. **패턴 중간에 `**`나 `{*var}` 금지** (끝에만 허용).
  `/api/**/admin`처럼 안 되던 규칙은 기동 시 예외.
- 커스텀 DSL 적용은 `.apply()` 대신 `.with()`.
- 기본으로 dispatch 요청도 인가 검사를 받으므로, 에러 페이지 등은
  `dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()`로 풀어 준다.
- Boot 4에서 CSRF는 폼 앱뿐 아니라 API POST/PUT/DELETE에도 기본 적용된다. stateless API라면
  연결 체인에서 명시적으로 끈다.
- 메서드 보안은 `@EnableMethodSecurity` + `@PreAuthorize`.

```java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/reviews/**").permitAll()
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form.loginPage("/login").permitAll())
            .logout(logout -> logout.logoutSuccessUrl("/login?logout"))
            .rememberMe(Customizer.withDefaults());
        return http.build();
    }
}
```

테스트 스타터: `spring-boot-starter-security-test` (보안 슬라이스 테스트. `@TestSecurity`, `@WithMockUser` 등).

## 5. Spring Data JPA 4 / Hibernate 7

- 정적 메타모델: `hibernate-jpamodelgen` → **`org.hibernate.orm:hibernate-processor`** (클래스도
  `org.hibernate.processor.HibernateProcessor`로 이동). 버전은 Boot BOM이 관리.
- 파생 쿼리 메서드는 Criteria API 대신 **JPQL 문자열**로 생성된다. 시그니처는 동일.
  정렬·단일 결과 동작에 의존하는 조회는 회귀 테스트 대상.
- 단일 결과: 내부적으로 `Query.getSingleResultOrNull()`을 쓰므로 `NoResultException` 캐치가 죽은 코드가 된다.
- null성: Spring `@Nullable`/`@NonNullApi` 대신 **JSpecify `@NullMarked`** 체계. 패키지 단위로
  도입하고 `@NullMarked` 하에서 null 파라미터는 실행 시 `IllegalArgumentException`.
- **record는 값 객체(`@Embeddable`)·프로젝션·DTO에만.** `@Entity`에 record를 쓰면 부트스트랩 실패.
- 제거: `spring.data.jpa.query.native.parser` (→ `QueryEnhancerSelector`), `@PersistenceConstructor`
  (→ `@PersistenceCreator`), `PropertyPath`/`TypeInformation`, `ListenableFuture`(→ `CompletableFuture`).

```java
@NullMarked
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByStoreIdOrderByCreatedAtDesc(Long storeId); // 파생 쿼리 = JPQL
    Optional<Review> findByMemberIdAndStoreId(Long memberId, Long storeId);
}
```

JPQL 표준 연산자(`||`, `cast()`, `union`/`intersect`/`except`, `left()`/`right()`/`replace()`)는
JPA 3.2 기준이라 provider 간 이식 가능. JSON/XML 함수(`json_object()` 등)는 Hibernate 전용.

## 6. Testcontainers 2.x

모듈 좌표와 패키지가 모두 바뀌었다. 구 좌표는 Boot 4 BOM에 없어서 의존성 해결이 바로 실패한다.

| 대상 | 1.x | 2.x |
| --- | --- | --- |
| JUnit 5 연동 | `org.testcontainers:junit-jupiter` | `org.testcontainers:testcontainers-junit-jupiter` |
| PostgreSQL | `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` |
| MySQL | `org.testcontainers:mysql` | `org.testcontainers:testcontainers-mysql` |
| 클래스 | `org.testcontainers.containers.PostgreSQLContainer<?>` | `org.testcontainers.postgresql.PostgreSQLContainer` (제네릭 없음) |

서비스 커넥션(`@ServiceConnection`)은 `spring-boot-testcontainers` 모듈이 제공한다.
커넥션 팩토리(예: `PostgreSQLContainer` → `JdbcConnectionDetails`)는 SQL/JDBC 스타터가 제공하므로,
JDBC·JPA를 쓴다면 `spring-boot-starter-jdbc`(슬라이스 테스트는 `-jdbc-test`)가 있어야 팩토리를 찾는다.

```java
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ReviewIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    // 컨테이너 준비 순서를 코드에서 직접 별도로 넣거나
    // 커넥션 세부 정보를 붙이지 않아도 DataSource 자동 구성이 컨테이너를 바라본다.
}
```

## 7. Docker Compose v2

- 파일 `compose.yml`, 명령 `docker compose`(공백, v1 `docker-compose`는 EOL).
- 상단 `name:`으로 프로젝트명 고정.
- `depends_on`은 `condition: service_healthy` + `healthcheck`로 준비 상태를 기다린다.
- 개발용 재시작 없이 파일 동기화는 `develop.watch`(sync/rebuild/sync+restart), 명령은 `docker compose up --watch`.
- 환경 분기는 프로파일(`--profile`)과 `env_file`/`.env`로, 비밀값은 `secrets`로.
- 프로덕션 이미지는 멀티스테이지 + 비루트 `USER` + named volume.

```yaml
name: tasreview

services:
  app:
    build:
      context: .
      target: runtime
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/tasreview
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
    depends_on:
      db:
        condition: service_healthy
    restart: unless-stopped

  db:
    image: postgres:16-alpine
    volumes:
      - postgres_data:/var/lib/postgresql/data
    env_file: .env
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER} -d tasreview"]
      interval: 10s
      timeout: 5s
      retries: 5
    restart: unless-stopped

volumes:
  postgres_data:
```

Dockerfile은 멀티스테이지로 `builder`(빌드·테스트)와 `runtime`(JRE·JAR만)을 나누고
`runtime`에서 `USER` 비루트로 실행한다. 빌더는 BuildKit 기본이라 캐시·병렬 빌드가 활성화되어 있다.

## 8. 자주 틀리는 마이그레이션 실수

| 아는 옛 문법 | Boot 4 실제 | 실패 형태 |
| --- | --- | --- |
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` | 클래스/빈 누락 |
| `spring-boot-starter-test`가 MockMvc 제공 | 전용 `-test` 스타터 필요 | `@Autowired MockMvc` UnsatisfiedDependency |
| `.and()` 연쇄 / `authorizeRequests` | 람다 DSL `authorizeHttpRequests` | 컴파일 실패 |
| `antMatchers("/x/**")` | `requestMatchers(...)` + 중간 `**` 금지 | 기동 예외 또는 조용한 미매칭 |
| `@MockBean` | `@MockitoBean` | 권장 교체(구은 deprecated) |
| `hibernate-jpamodelgen` | `hibernate-processor` | 의존성 미해결 / 메타모델 미생성 |
| `@NonNullApi`(Spring) | JSpecify `@NullMarked` | (없음 - 조용한 동작 차이) |
| `testcontainers:postgresql` | `testcontainers-postgresql` | 의존성 해결 실패 |
| docker-compose `version:` 키, `docker-compose` 명령 | 필요 없음, `docker compose` | 경고/옛 동작 |