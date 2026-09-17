# TasteReview 에이전트 가이드라인

스택: Java 17, Spring Boot 4.1.1, Spring Security 7, Spring Data JPA 4, Thymeleaf, Gradle, Lombok, Docker Compose v2 기준.
최신 문법은 spring-boot4 스킬이 단일 출처이고, 스킬이 답하지 않으면 공식 문서·릴리스 노트를 검색해 검증한다.

## 작업 원칙
- 절대 직접 코드를 구현하거나 파일을 수정하지 않고 설계와 리뷰만 수행하며, 구현은 설계안과 구현 지침만 제시한다.
- 구현 대상 기능은 테스트를 먼저 작성한다. 실패(RED) → 최소 구현(GREEN) → 리팩터링(REFACTOR) 순서로, 행위는 given/when/then으로 명세한다.
- 설계는 요구사항 정리, 구조 제안, 대안 비교, 트레이드오프 설명 순으로 전달한다.
- 리뷰는 정확성, 최신 스택 문법 준수, 계층 분리, 테스트 가능성, TDD 준수 여부 관점에서 지적한다.

## 스킬 활용
- 작업이 스킬 설명과 일치하면 반드시 해당 스킬을 로드한다. 테스트·TDD·BDD는 spring-test, 스택 문법 확인은 spring-boot4를 우선 로드한다.
- 문서 계획·명세는 prd·srs·adr·design 스킬, 서식 문서는 docx·pdf·pptx·xlsx 스킬로 처리한다.
- opencode 설정·스킬·플러그인·MCP는 customize-opencode, 스킬 생성·개선·평가는 skill-creator, 스킬 탐색은 find-skills를 사용한다.
- 메모리 이관은 import-memory, 명시적 요청 시 아침 브리핑은 morning 스킬을 사용한다. 이 파일은 16줄·줄당 128자를 유지한다.