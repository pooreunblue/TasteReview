# SRS 인덱스

TasteReview의 소프트웨어 요구사항 명세서(SRS)를 모아 둔 디렉터리입니다.
"왜 만드는가"는 [PRD](../prd/README.md)에, "시스템이 어떻게 행동해야 하는가"는 여기에 둡니다.

## 목록

| 번호 | 제목 | 상태 | 출처 PRD | 최종 수정일 | 문서 |
| --- | --- | --- | --- | --- | --- |
| 0001 | 맛집 리뷰 서비스 MVP | Draft | [0001](../prd/0001-taste-review-mvp.md) | 2026-09-17 | [0001-taste-review-mvp.md](0001-taste-review-mvp.md) |

## 규칙

- 파일명은 `NNNN-slug.md` 형식입니다. `NNNN`은 4자리 일련번호, `slug`는 영문 소문자 kebab-case입니다.
- 번호는 PRD 번호와 별개로 매기고, 대응 관계는 문서 프런트매터의 `출처 PRD` 필드와 위 표에 남깁니다.
- 상태는 `Draft` → `Review` → `Baselined` → `Superseded` 순입니다. `Baselined`는 구현 착수 기준선이라는 뜻입니다.
- 문서를 추가하거나 상태를 바꿀 때마다 위 표를 함께 갱신합니다.
- 새 문서는 `.agents/skills/srs/assets/srs-template.md` 템플릿을 복사해 작성합니다.

## 작성 방법

Claude Code에서 `/srs` 스킬을 사용하거나, "PRD를 구현 가능한 수준으로 풀어 줘"처럼 요청하면 선행 PRD를 읽고 위 규칙에 맞춰 명세를 작성한 뒤 인덱스를 갱신합니다.
