# ADR-006b · 카카오 알림 재시도/DLQ ETA

## Status
**Implemented (Outbox 옵션 채택, 2026-05-17)** — PR #16 에서 §2-(ii) Outbox 옵션 활성화. 비동기 (`@Async`) 와 외부 큐 (Kafka/SQS) 는 여전히 범위 외.

(원래 박제: Accepted — 본 리팩토링 범위 외 (V3-D))

## Context
- ADR-006a 채택 후에도 카카오 API 호출은 **요청 스레드에서 동기 실행** (`@TransactionalEventListener` 만으로는 비동기화 아님).
- 카카오 API 지연 시 사용자 응답이 그만큼 지연.
- 알림 실패 시 재시도/DLQ 없음 → 항구적 실패.

## Decision (결정만 박제, 구현 후순위)

### 1) 비동기화 (활성화 시)
- `@Async("kakaoNotificationExecutor")` 어노테이션 추가
- `ThreadPoolTaskExecutor` 빈 신설:
  ```
  @Bean(name = "kakaoNotificationExecutor")
  public ThreadPoolTaskExecutor kakaoNotificationExecutor() {
      ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
      executor.setCorePoolSize(2);
      executor.setMaxPoolSize(4);
      executor.setQueueCapacity(100);
      executor.setThreadNamePrefix("kakao-notif-");
      return executor;
  }
  ```
- `@EnableAsync` 활성화

### 2) 재시도/DLQ 옵션 (활성화 시 선택)
- **(i) `@Retryable`** (Spring Retry) — 간단, 인메모리, 재시작 시 손실
- **(ii) Outbox 테이블** — `outbox_events` 테이블 + 스케줄러 — durable, 정확히-한-번 보장
- **(iii) 외부 큐 (Kafka/SQS)** — 학습 과제 범위 명백히 초과

## ETA / 활성화 조건 (V3-D)

> **본 ADR 구현은 본 리팩토링 사이클 외**. 다음 중 하나가 충족될 때 활성화:
> - **다음 사이클** (5주차 과제 완료 후 후속 작업)
> - **운영 이벤트**: 카카오 알림 응답 시간 SLA 위반 (예: p95 > 2초) 또는 알림 실패율 SLA 위반 (예: 24시간 > 1%)

## Drivers
- 사용자 응답 시간 보호
- 알림 신뢰성 향상
- 그러나 학습 과제 범위 vs 인프라 복잡도 트레이드오프 → 본 사이클 보류

## Alternatives considered
- 본 사이클에 구현: prd "한 번에 한 조각" + 학습 범위와 충돌. 탈락.
- ADR-006a 와 통합: 결정 분리가 향후 활성화 결정에 명확. 탈락 → 분리.

## Why chosen
- 결정만 박제, 구현 ETA 명문화 → executor 가 본 사이클에서 `@Async` 를 만들지 결정 시 명확.
- 운영 트리거 조건 명시로 후속 활성화가 자동 판단 가능.

## Consequences
- ADR-006a 의 사용자 응답 지연 문제는 본 사이클에서 미해결.
- 운영 모니터링 도입 시 활성화 트리거 자동 감지 가능.

## Follow-ups
- 카카오 알림 응답 시간 메트릭 수집 (Micrometer + Prometheus 등) — 별도 작업.
- 활성화 시 Outbox vs Retryable 선택 — 운영 트래픽 분석 후 결정.

## 적용 PR
- **PR #16 (2026-05-17): §2-(ii) Outbox 옵션 채택 완료.**
  - Flyway V3 `outbox_event` 테이블 (status / attempts / last_error / processed_at + status 인덱스).
  - `OutboxEvent` JPA 엔티티 + `OutboxEventRepository`.
  - `OutboxEventProcessor` @Transactional public method — kakao 호출, 성공 시 SENT, 실패 시 attempts 증가, MAX_ATTEMPTS(5) 도달 시 DEAD. 예외 swallow.
  - `OutboxPoller` @Scheduled(fixedDelay=5000) — 50건 배치 처리, 각 행을 별도 tx 로 위임.
  - `Application.java` 에 `@EnableScheduling` 부착.
  - `OrderService.placeOrder` 가 인메모리 이벤트 발행 대신 outbox 행 INSERT (같은 tx).
  - 폐기: `KakaoNotificationListener` (인메모리 AFTER_COMMIT).
  - 회귀 보호: `OrderServiceIntegrationTest.placeOrderWritesPendingOutboxRow` + `OutboxEventProcessorTest` 4건.
- 06-order §B.5 의 "본 사이클 외" 박제는 PR #16 으로 활성화됨.
- 비동기 (`@Async` + Executor) 와 외부 큐는 다음 사이클로 연기.
