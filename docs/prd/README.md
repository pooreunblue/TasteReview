# PRD 인덱스

TasteReview의 제품 요구사항 문서(PRD)를 모아 둔 디렉터리입니다.

## 목록

| 번호 | 제목 | 상태 | 최종 수정일 | 문서 |
| --- | --- | --- | --- | --- |
| 0001 | 맛집 리뷰 서비스 MVP | Draft | 2026-09-17 | [0001-taste-review-mvp.md](0001-taste-review-mvp.md) |

## 규칙

- 파일명은 `NNNN-slug.md` 형식입니다. `NNNN`은 4자리 일련번호, `slug`는 영문 소문자 kebab-case입니다.
- 한 기능당 한 문서를 유지하고, 내용이 바뀌면 새 파일을 만들지 말고 기존 문서를 갱신합니다.
- 상태는 `Draft` → `Review` → `Approved` → `In Progress` → `Done` 순으로 바뀌며, 폐기된 문서는 `Dropped`으로 남겨 둡니다.
- 문서를 추가하거나 상태를 바꿀 때마다 위 표를 함께 갱신합니다.
- 새 문서는 `.agents/skills/prd/assets/prd-template.md` 템플릿을 복사해 작성합니다.

## 작성 방법

Claude Code에서 `/prd` 스킬을 사용하거나, "새 기능 PRD 써 줘"처럼 요청하면 위 규칙에 맞춰 문서를 만들고 인덱스를 갱신합니다.
