# ADR 인덱스

TasteReview의 아키텍처 결정 기록(ADR)을 모아 둔 디렉터리입니다.
"왜 만드는가"는 [PRD](../prd/README.md), "시스템이 어떻게 행동해야 하는가"는 [SRS](../srs/README.md), "그것을 구현하려고 어떤 구조와 기술을 택했고 무엇을 버렸는가"는 여기에 둡니다.

## 목록

| 번호 | 제목 | 상태 | 결정일 | 문서 |
| --- | --- | --- | --- | --- |
| 0001 | 화면을 Thymeleaf 서버 렌더링으로만 제공한다 | Accepted | 2026-09-17 | [0001-server-side-rendering.md](0001-server-side-rendering.md) |
| 0002 | Render에 컨테이너 이미지로 배포한다 | Accepted | 2026-09-17 | [0002-render-container-deployment.md](0002-render-container-deployment.md) |
| 0003 | 저장소로 Neon PostgreSQL을 사용한다 | Accepted | 2026-09-17 | [0003-neon-postgresql.md](0003-neon-postgresql.md) |
| 0004 | 리뷰 사진을 데이터베이스에 저장한다 | Accepted | 2026-09-17 | [0004-store-images-in-database.md](0004-store-images-in-database.md) |
| 0005 | 가게를 별도 도메인으로 만들지 않고 리뷰의 문자열로 둔다 | Accepted | 2026-09-17 | [0005-no-place-entity.md](0005-no-place-entity.md) |
| 0006 | 변경 가능한 모든 설정값을 환경변수로 외부화한다 | Accepted | 2026-09-17 | [0006-externalize-configuration.md](0006-externalize-configuration.md) |
| 0007 | MVP 구현 세부 선택을 확정한다 | Accepted | 2026-09-17 | [0007-mvp-implementation-decisions.md](0007-mvp-implementation-decisions.md) |

## 규칙

- 파일명은 `NNNN-slug.md` 형식입니다. `NNNN`은 4자리 일련번호이며 한 번 쓴 번호는 재사용하지 않습니다.
- 상태는 `Proposed` → `Accepted`, 이후 `Deprecated` 또는 `Superseded by NNNN`입니다.
- `Accepted` 이후에는 결정 내용을 고쳐 쓰지 않습니다. 결정이 바뀌면 새 ADR을 만들고 기존 문서를 `Superseded by NNNN`으로 표시해 상호 링크를 남깁니다.
- 한 문서에는 결정 하나만 담습니다. 둘 이상을 담으면 나중에 일부만 대체할 수 없습니다.
- 문서를 추가하거나 상태를 바꿀 때마다 위 표를 함께 갱신합니다.
- 새 문서는 `.agents/skills/adr/assets/adr-template.md` 템플릿을 복사해 작성합니다.

## 작성 방법

Claude Code에서 `/adr` 스킬을 사용하거나, "이 선택 기록으로 남겨 줘", "A와 B 중 뭘 쓸까"처럼 요청하면 관련 PRD·SRS의 요구사항을 동인으로 인용해 결정을 기록하고 인덱스를 갱신합니다.
