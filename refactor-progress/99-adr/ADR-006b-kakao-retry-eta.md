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

## Outbox 채택 근거 (PR #16, §2-ii 선택)

세 옵션 중 **(ii) Outbox 테이블**을 택한 이유 — 코드 근거와 함께:

### 1) 주문 ↔ 알림 원자성 (dual-write 문제 해소) — 핵심
`OrderService.placeOrder`(`OrderService.java:44-63`)가 주문 저장과 알림 적재를 **하나의 `@Transactional`** 안에서 수행한다:
- `orderRepository.save(...)` (주문)
- `outboxEventRepository.save(new OutboxEvent(...))` (알림, `OrderService.java:89`)

두 INSERT가 같은 DB 트랜잭션이므로 **"주문은 커밋됐는데 알림은 유실"** 혹은 그 반대가 원천 차단된다. 직전 방식인 인메모리 `@TransactionalEventListener(AFTER_COMMIT)`(폐기된 `KakaoNotificationListener`)는 커밋과 외부 전송 사이에 프로세스가 죽으면 알림이 사라지는 dual-write 취약점이 있었다.

### 2) 내구성 / 재시작 생존
알림 의도가 DB 행(`outbox_event`, status=`PENDING`)으로 영속화된다. 앱이 죽었다 살아나도 `OutboxPoller`(`@Scheduled(fixedDelay=5000)`, `OutboxPoller.java:21`)가 PENDING 행을 다시 집어간다. **(i) `@Retryable`**(인메모리)은 재시작 시 진행 중인 재시도가 통째로 손실 → 탈락.

### 3) 재시도 + DLQ 격리를 DB만으로 구현
`OutboxEventProcessor.processOne`(`OutboxEventProcessor.java:29-47`)이:
- 성공 → `markSent()` (status=`SENT`)
- 실패 → `markFailure()`로 `attempts++` + `last_error` 보존, `MAX_ATTEMPTS(5)` 도달 시 status=`DEAD`

즉 `DEAD`가 **DLQ 역할**을 하고, `last_error`/`attempts` 컬럼이 실패 관찰성을 준다. 외부 메시지 브로커 없이 기존 DB만으로 at-least-once 전달 + 재시도 + DLQ를 확보 → **(iii) 외부 큐**의 인프라 비용 회피.

### 4) 사용자 응답 시간에서 외부 호출 분리
주문 트랜잭션에는 **outbox INSERT만** 포함되고, 실제 카카오 HTTP 호출은 폴러가 **별도 스레드 + 행별 별도 `@Transactional`**(`processOne`은 행 단위 메서드)로 수행한다. 카카오 API 지연/실패가 주문 응답을 막지 않는다 — ADR-006a에 남아 있던 "동기 호출 잔존" 문제를 함께 해소.

### 5) 학습 과제 범위 적합
DB·스케줄러만 추가하면 되고 별도 미들웨어(Kafka/SQS) 운영이 없다. 신뢰성 이득 대비 인프라 복잡도가 가장 낮은 지점.

### 트레이드오프 (감수한 비용)
| 항목 | 내용 |
|---|---|
| 폴링 지연 | `fixedDelay=5000` → 알림이 최대 ~5초 지연. 실시간성 요구 낮은 알림이라 수용. |
| at-least-once → 중복 | 카카오 호출 성공 후 `markSent` 커밋 전 장애 시 재전송 가능. **현재 멱등키 없음** → 중복 메시지 가능성 잔존(후속 과제). |
| 폴링 DB 부하 | `idx_outbox_status`로 PENDING 스캔 최적화. 5초마다 top-50 배치로 부하 제한. |
| 단일 인스턴스 가정 | 다중 인스턴스 시 같은 행을 동시 집을 수 있음(`SELECT ... FOR UPDATE SKIP LOCKED` 미적용). 현 단계 단일 인스턴스라 미도입. |

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
