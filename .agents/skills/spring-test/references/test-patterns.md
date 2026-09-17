# 테스트 패턴 참조 (Spring Boot 4, TDD·BDD)

given/when/then 시나리오의 골격과 Boot 4 문법대로 쓴 슬라이스·통합 테스트 예제다.
이 스킬의 지침을 받은 요청이면 이 파일의 골격을 그대로 쓴다. 문법 판단은 spring-boot4 스킬을 따른다.

## 1. BDD 명명 규칙

- 영어: `givenXxxWhenYyy_thenZzz` — 예: `givenRegisteredMember_whenSubmitsReview_thenReviewListed`
- 한글: `전제_when_결과` — 예: `게시글이_있을때_댓글을_달면_댓글수가_1_증가한다`
- 시나리오 하나 = 테스트 메서드 하나 = 핵심 어서션 하나. 검증이 두 개 이상 필요하면
  그것이 같은 사실을 확인하는 것인지 다시 묻는다.

## 2. 단위(서비스) 테스트 — Mockito BDD

| 구성 요소 | 사용 |
| --- | --- |
| 테스트 툴 | JUnit 5 (`org.junit.jupiter`) |
| 검증 | AssertJ (`assertThat(...)`) |
| 목 | Mockito BDD (`given(...).willReturn(...)`, `willThrow(...)`) |
| 협력 목 주입 | `@ExtendWith(MockitoExtension.class)` + `@Mock`, 수동 생성 |

```java
@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    ReviewRepository reviewRepository;

    @InjectMocks
    ReviewService reviewService;

    @Test
    void givenMember_whenSubmitsReview_thenReviewIsSaved() {
        // given
        ReviewDraft draft = new ReviewDraft(1L, 1L, 5, "맛있어요");
        given(reviewRepository.save(any(Review.class))).willAnswer(invocation -> invocation.getArgument(0));

        // when
        Review saved = reviewService.submit(draft);

        // then
        assertThat(saved.getStar()).isEqualTo(5);
        then(reviewRepository).should().save(any(Review.class));
    }
}
```

- 목은 행위를 검증하는 마지막 수단이다. 반환값이 있는 협력은 상태 검증을 우선한다.
- `willAnswer` 대신 검증에 필요한 값만 반환하도록 단순하게 유지한다.

## 3. 웹 슬라이스 — @WebMvcTest + @MockitoBean

Boot 4 패키지 기준(`org.springframework.boot.webmvc.test.autoconfigure`)을 쓰고,
컨트롤러 협력 서비스는 Framework 7의 `@MockitoBean`으로 목 처리한다.

```java
@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @MockitoBean
    ReviewService reviewService;

    @Autowired
    MockMvc mockMvc;

    @Test
    void givenStoredReviews_whenListThem_thenRendersReviewList() throws Exception {
        // given
        given(reviewService.listAll()).willReturn(List.of(new ReviewSummary(1L, "한식", 5)));

        // when & then
        mockMvc.perform(get("/reviews"))
            .andExpect(status().isOk())
            .andExpect(view().name("review/list"))
            .andExpect(model().attribute("reviews", hasSize(1)));
    }

    @Test
    void givenGuest_whenPostReview_thenRedirectsToLogin() throws Exception {
        // when & then (시큐리티 미적용 슬라이스에서는 CSRF/인가를 함께 검증)
        mockMvc.perform(post("/reviews").with(csrf()))
            .andExpect(status().is3xxRedirection());
    }
}
```

- 슬라이스에 시큐리티를 넣으면 `spring-boot-starter-security-test`의 `@WithMockUser`·`csrf()`를 쓸 수 있다.
- 시큐리티 슬라이스 검증 시에는 `spring-boot4` 스킬의 Security 7 문법(람다 DSL·requestMatchers)과
  CSRF 기본 적용을 함께 확인한다.

## 4. 영속 슬라이스 — @DataJpaTest + Testcontainers

로컬 개발은 H2로 빠르게, CI·운영 근접 검증은 Testcontainers로 나눈다.
`@DataJpaTest`는 기본 롤백이므로 저장 확인 테스트도 이 안에서 처리한다.

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ReviewRepositoryTest {

    @Autowired
    ReviewRepository reviewRepository;

    @Test
    @Transactional
    void givenStoreId_whenFindByStore_thenOnlyThatStoresReviewsReturned() {
        // given
        reviewRepository.saveAll(List.of(
            new Review("가게A", "a"),
            new Review("가게B", "b")
        ));

        // when
        List<Review> found = reviewRepository.findByStoreIdOrderByCreatedAtDesc(1L);

        // then
        assertThat(found).extracting(Review::getStoreName).containsExactly("가게A");
    }
}
```

- `@NullMarked`(JSpecify) 패키지에서 null 인자 전달은 실행 시 예외다. 경계를 검증할 테스트를 둔다.
- 파생 쿼리는 JPQL 문자열로 생성되므로, 정렬·중복·null 순서에 의존하는 메서드는
  반환 순서를 어서션으로 고정한다(문자열이 아니라 결과 순서 기준).

## 5. 통합 테스트 — @SpringBootTest + @AutoConfigureMockMvc + Testcontainers

Boot 4에서 `@SpringBootTest`만으로는 MockMvc를 주입받지 못한다. 반드시 `@AutoConfigureMockMvc`를 붙인다.

```java
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ReviewIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    MockMvc mockMvc;

    @Test
    void givenStoredReviewInPostgres_whenList_thenReviewShown() throws Exception {
        // given: Testcontainers DB에 직접 시드(POST를 쓰면 상태 변이가 전파됨)

        // when & then
        mockMvc.perform(get("/reviews"))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("맛있어요")));
    }
}
```

- `@ServiceConnection` 커넥션 팩토리는 JDBC 스타터가 제공한다. `spring-boot-starter-jdbc`
  (슬라이스는 `-jdbc-test`)가 classpath에 있어야 한다. 좌표 검증은 spring-boot4 스킬 표를 따른다.
- 통합 테스트는 느리다. 시나리오가 슬라이스로 검증 가능하면 슬라이스를 우선한다.

## 6. 실행 확인

```bash
./gradlew test
```

- RED 확인: 새 테스트가 실패하거나 컴파일되지 않는 상태를 먼저 본다.
- GREEN 확인: 구현 다음에 테스트 전체와 여는 메시지가 깨지지 않는지 본다.
- 항상 JUnit Platform으로 실행한다(`useJUnitPlatform()`).