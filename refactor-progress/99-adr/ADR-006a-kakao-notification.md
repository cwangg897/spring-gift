# ADR-006a · 카카오 알림 실패 정책 + 테스트 함정

## Status
Accepted (2026-05-16)

## Context
- `OrderController.java:108-112`: `try { kakaoMessageClient.sendToMe(...) } catch (Exception ignored) {}` — 모든 예외를 침묵 삼킴 (관찰 불가).
- `KakaoMessageClient.sendToMe` (`KakaoMessageClient.java:16-29`): 외부 HTTP 동기 호출 → 응답 지연 사용자에게 전파.
- 01.5에서 임시로 WARN 로깅 추가됨, 06 Phase B에서 정식 이벤트화 결정.

## Decision

### 1) 트랜잭션 외부화
- `OrderCompletedEvent(orderId, memberId, optionId, quantity)` record 신설.
- `OrderService.placeOrder` 가 `ApplicationEventPublisher.publishEvent(...)` 호출.
- `gift.order.KakaoNotificationListener` 가 `@TransactionalEventListener(phase=AFTER_COMMIT)` 부착, 커밋 후 비동기 실행은 별도 (ADR-006b).

### 2) 실패 정책 (V3-5 박제 4항목)

| # | 정책 | 이유 |
|---|---|---|
| (a) | **주문 트랜잭션은 살림** | AFTER_COMMIT 이므로 이미 커밋됨. 알림 실패가 주문 롤백을 일으키지 않음. |
| (b) | **별도 트랜잭션 없음** | 알림 핸들러는 `@Transactional` 미부착. DB 접근 없음, HTTP 호출만. |
| (c) | **로깅 only — WARN 수준** | `log.warn("Kakao notification failed: orderId={}, memberId={}", orderId, memberId, ex)`. token 값은 redact. |
| (d) | **재시도 없음** | 본 사이클 범위 외. 재시도/DLQ 는 ADR-006b. |

### 3) 테스트 함정 박제 (V3-5)

> **함정**: Spring `@Transactional` 테스트 안에서는 테스트 종료 시 트랜잭션이 **롤백**된다. AFTER_COMMIT 이벤트는 커밋 후 발화하므로 **트랜잭션 안 테스트에서는 이벤트가 발화하지 않는다** (false-green).
>
> **회피 패턴** (06-order Phase B.2 테스트에 필수 적용):
> - `@Transactional` **부착 금지** (테스트 클래스/메서드 둘 다)
> - DB 정리: `@Sql(scripts="/cleanup-orders.sql", executionPhase=AFTER_TEST_METHOD)`
> - 이벤트 검증: `@RecordApplicationEvents` + `ApplicationEvents.stream(OrderCompletedEvent.class)`
> - 외부 HTTP mocking: `MockRestServiceServer` 또는 `WireMock`
>
> ADR-001 격리정책 매트릭스에도 동일 박제.

### 4) 검증 시나리오 (06-order Phase B.2 통합 테스트)

| # | 시나리오 | 기대 결과 |
|---|---|---|
| 1 | 주문 정상 커밋 | `OrderCompletedEvent` 발화 + `KakaoMessageClient` 호출 + 200 응답 |
| 2 | 주문 트랜잭션 롤백 (예: 포인트 부족) | 이벤트 **미발화** + Kakao 호출 0건 + 400/422 응답 |
| 3 | 주문 커밋, 카카오 API 실패 | 주문 응답 정상(200/201) + WARN 로그 출력 + 주문 데이터 보존 |

## Drivers
- 트랜잭션 정합성 (알림 실패 ≠ 주문 실패)
- 관찰 가능성 (WARN 로깅으로 실패 추적 가능)
- 학습 과제 범위 (재시도/DLQ 는 운영 영역)

## Alternatives considered
- **현 상태 유지 (`Exception ignored`)**: 관찰 불가, 디버깅 비용. 탈락.
- **카카오 호출을 service 안 동기 호출**: 카카오 API 실패가 주문 실패로 전파. 탈락.
- **Outbox + 메시지 큐**: 학습 과제 범위 초과. 탈락 (ADR-006b 후속).

## Why chosen
- AFTER_COMMIT 으로 트랜잭션 정합성 확보 + WARN 로깅으로 관찰성 확보.
- 학습 과제 범위 내에서 운영 기본기.

## Consequences
- 카카오 API 지연 시 사용자 응답이 여전히 지연될 수 있음 (동기 실행 잔존). ADR-006b 에서 `@Async` 로 해결.
- WARN 로그 모니터링 필요 (별도 운영 가이드).
- AFTER_COMMIT 함정 인지 못한 테스트가 false-green 위험 — 본 ADR 명문화로 차단.

## Follow-ups
- ADR-006b 활성화 시 `@Async` + Executor 추가.
- 알림 실패율 SLA 정의 (운영 단계).
- 카카오 API 응답 시간 메트릭 수집.

## 적용 PR
- 01.5-tx-boundary §6.4 (선제 WARN 로깅)
- 06-order Phase B.2, B.3 (이벤트화 + 시나리오 검증)
