# ADR-006b · 카카오 알림 재시도/DLQ ETA (범위 외)

## Status
Accepted — **본 리팩토링 범위 외** (V3-D 박제)

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
- (해당 없음, 후속 사이클)
- 06-order §B.5 에 "결정만 박제, 구현 본 사이클 외" 명시
