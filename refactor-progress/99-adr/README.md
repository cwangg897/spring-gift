# Architecture Decision Records · spring-gift refactor

> 모든 ADR은 다음 6 필드를 채운다: **Status / Context / Decision / Drivers / Alternatives considered / Why chosen / Consequences / Follow-ups**.

## ADR 인덱스

| ADR | 제목 | 상태 | 적용 PR |
|---|---|---|---|
| [ADR-001](./ADR-001-testcontainers-isolation.md) | Testcontainers + 격리정책 + AFTER_COMMIT 함정 | Accepted | 00, 06 |
| [ADR-002](./ADR-002-tx-boundary.md) | 트랜잭션 경계 = 서비스 계층 | Accepted | 01a, 01b, 01.5, 02~06 |
| [ADR-003](./ADR-003-fk-mapping.md) | JPA `@ManyToOne(LAZY)` 통일 + N+1 정책 + V1 스키마 사실 | Accepted | 04.5 |
| [ADR-004](./ADR-004-validation-matrix.md) | 검증 책임 매트릭스 + anti-pattern ban | Accepted | 01a, 02, 04 |
| ~~ADR-005~~ | (폐기, ADR-001 에 흡수) | Superseded | — |
| [ADR-006a](./ADR-006a-kakao-notification.md) | 카카오 알림 실패 정책 + 테스트 함정 | Accepted | 06 |
| [ADR-006b](./ADR-006b-kakao-retry-eta.md) | 카카오 알림 재시도/DLQ ETA | Accepted (범위 외) | 후속 |
| [ADR-007](./ADR-007-concurrency.md) | 동시성 제약 + 예외 계층 통합 | Accepted | 01b, 02, 04, 05 |

## ADR 작성 조건 (prd 라인 37-41)

다음 중 하나라도 해당되면 ADR을 남긴다:
- 선택지가 2개 이상이고 트레이드오프가 존재한 경우
- 반복적으로 따라야 할 규칙이나 경계를 정의한 경우
- 테스트 전략이나 검증 방식이 결정의 핵심이었던 경우

## 변경 이력

- 2026-05-16: v3 plan 합의 — ADR-001~007 (ADR-005 폐기) 박제
