# 06-order · 주문 도메인 (Phase A+B 합본, V3-E 박제)

> **PR #15 / 15** (최종 도메인 PR).
> **PR 합본 이유**: Phase A의 `OrderFacade → OrderService` 승격은 V3-E 영구화 차단의 핵심이고, Phase B는 그 위에서 카카오 이벤트화 + 누락 동작 구현. 단일 PR 내 2 단계 커밋(승격 + 이벤트화 + facade 제거)으로 진행.

---

## [필수 1] 0단계 그린 캡처

- 직전 PR: `05-wish Phase B` (PR #14)
- 그린 캡처: 05 + 04.5 + 01.5(OrderFacade 도입 시점) 모두 메인에서 그린 — 커밋 SHA 첨부

---

## [필수 2] 현재 상태 진단

- 패키지: `gift.order`
- 대상 파일:
  - `src/main/java/gift/order/OrderFacade.java` (01.5에서 도입, `@Deprecated(forRemoval=true)` 박제됨) — Phase A에서 정식 service로 승격 후 제거
  - `src/main/java/gift/order/OrderController.java:69-102` — 01.5 이후 facade 위임 구조. service 승격 후 의존 클린업 필요
  - `src/main/java/gift/order/OrderController.java:104-113` — `sendKakaoMessageIfPossible`: 01.5에서 WARN 로깅으로 변경됨. Phase B에서 이벤트화 (ADR-006a)
  - `src/main/java/gift/order/KakaoMessageClient.java:16-29` — `sendToMe` 외부 HTTP, 본 사이클에서는 동기 유지(ADR-006b는 후속)
  - **누락 동작 (prd 4.3)**: `OrderController` 의 6단계 흐름에 "위시 정리(`wish cleanup`)"가 주석만 있고 실제 호출 없음 (`OrderController.java:67-68` 주석) → 05-wish의 `removeByMemberAndProduct` 호출 추가 필요
- 외부 의존: 01a(MemberService), 04(OptionService), 05(WishService), `KakaoMessageClient`(외부 API), `ApplicationEventPublisher`(Spring core)

---

## [필수 3] 목표 산출물 + 체크리스트

### Phase A (커밋 A 시리즈) — facade 승격 + V3-E 폐기

- [ ] **A.1** `gift.order.OrderService` 신설 — (a) `OrderFacade.createOrder` 본체를 그대로 이전, `@Transactional` 유지, 이름과 책임 명확화 (b) `OrderController` 가 `OrderService.placeOrder(...)` 호출, `OrderFacade` 참조 grep 결과 0건 (c) `OrderServiceTest` 그린
- [ ] **A.2** 의존 클린업 — (a) `OrderService` 가 `MemberService.findById`, `OptionService.subtractQuantity` 호출(이미 엔티티 메서드), `WishService.removeByMemberAndProduct` 호출 (B.1에서 본격 추가) — repository 직접 호출 grep 결과 service 에서 0건 (b) `OrderController` 가 `OrderService` 만 주입 (현재 6빈 → 1빈) (c) 회귀 보호 테스트
- [ ] **A.3** **`OrderFacade` 폐기 (V3-E 3중 차단)** — (a) `OrderFacade.java` 파일 삭제 (b) `grep -rn "OrderFacade" src/` **결과 0건** 확인, PR description에 캡처 (c) 본 PR 내부 별도 커밋(`refactor(order): remove OrderFacade after promotion to OrderService`)으로 분리 — 승격 커밋과 폐기 커밋 분리하여 history 명확화

### Phase B (커밋 B 시리즈) — 누락 동작 구현 + 이벤트화

- [ ] **B.1** **위시 정리 누락 동작 구현 (prd 4.3)** — (a) `OrderService.placeOrder` 내부에서 `wishService.removeByMemberAndProduct(member, option.getProduct().getId())` 호출 (b) `OrderController.java:67-68` 주석 제거 (코드로 실현됨) (c) 통합 테스트: 주문 성공 시 해당 product 의 wish 가 삭제됨, 주문 실패 시 wish 유지(트랜잭션 롤백)
- [ ] **B.2** **카카오 알림 이벤트화 (ADR-006a)** — (a) `OrderCompletedEvent(orderId, memberId, optionId, quantity)` record 신설, `OrderService` 가 `ApplicationEventPublisher.publishEvent(new OrderCompletedEvent(...))` 호출 (b) `gift.order.KakaoNotificationListener` 신설, `@TransactionalEventListener(phase=AFTER_COMMIT)` + `KakaoMessageClient.sendToMe` 호출. 실패 시 **WARN 로깅 only, 재시도 없음** (ADR-006a 4항목) (c) **테스트 함정 회피 (V3-5)**: 발화 검증 테스트는 `@Transactional` 없이 + `@Sql` cleanup + `@RecordApplicationEvents`. 시나리오 ≥3: ① 주문 성공 → 이벤트 발화 + 알림 호출 ② 주문 트랜잭션 롤백 → 이벤트 미발화 ③ 카카오 API 실패 → 주문은 커밋, WARN 로깅
- [ ] **B.3** `KakaoMessageClient` 의 try/catch 제거 — (a) 호출자(`KakaoNotificationListener`)가 try/catch 로 wrap + WARN 로깅. client 자체는 예외를 던지도록 명확화 (b) `OrderController.java:108-112` 의 잔재 `catch (Exception ignored)` 제거 (없으면 명시) (c) B.2 시나리오 ③ 그린
- [ ] **B.4** (선택) `Order.complete()` 같은 도메인 메서드 — 도메인 불변식이 명확할 때만. 현재 Order 엔티티는 생성자만으로 충분 → **본 사이클 건너뜀**, ADR 메모로 후속 후보

### ADR-006b 후순위 (V3-D 명문화)

- [ ] **B.5** (결정만 박제, 구현 본 사이클 외) `@Async("kakaoNotificationExecutor")` + `ThreadPoolTaskExecutor` 빈 — (a) — (b) — (c) ADR-006b에 ETA(다음 사이클 또는 SLA 위반 시) 박제. 본 PR에서는 구현하지 않음. README 비범위 표시.

---

## [필수 4] 검증 명령 + README 게이트

```
./gradlew test --tests "gift.order.*"
./gradlew test
grep -rn "OrderFacade" src/   # 반드시 0건
```

게이트 (V3-C, V3-E):
- [ ] PR #15 머지 시 README 진행률 갱신 (전체 15/15 완료)
- [ ] `OrderFacade` 0건 확인 + 별도 폐기 커밋 SHA 캡처
- [ ] 카카오 알림 시나리오 3건(성공/롤백/API 실패) 모두 그린 캡처

---

## [선택 1] 작동 변경 vs 구조 변경

| 변경 ID | 종류 | 관련 ADR |
|---|---|---|
| A.1, A.2 | 구조 (승격) | ADR-002 |
| A.3 | 구조 (폐기) | — |
| B.1 | 작동 (누락 동작 구현) | — |
| B.2, B.3 | 작동 (이벤트화 + 로깅) | ADR-006a, ADR-001 |
| B.4 | (보류) | — |
| B.5 | (결정만 박제) | ADR-006b |

## [선택 2] 시퀀스 다이어그램

```
Controller
  └─► OrderService.placeOrder(member, request) ┐
                                                 ├─ [ @Transactional ]
                                                 │     1. option.subtractQuantity()  (이미 도메인 메서드)
                                                 │     2. member.deductPoint()       (이미 도메인 메서드)
                                                 │     3. orderRepository.save(...)
                                                 │     4. wishService.removeByMemberAndProduct(...)  (B.1 신규)
                                                 │     5. eventPublisher.publishEvent(new OrderCompletedEvent(...))
                                                 └─ (트랜잭션 종료)
                                                       │
                                                       ▼
                                                 [ AFTER_COMMIT ]
                                                 KakaoNotificationListener
                                                   └─► KakaoMessageClient.sendToMe(...)
                                                         (실패 시 WARN 로깅, 재시도 없음)
```

## [선택 3] 관련 ADR

- [ADR-001](./99-adr/ADR-001-testcontainers-isolation.md) — AFTER_COMMIT 테스트 함정 박제
- [ADR-002](./99-adr/ADR-002-tx-boundary.md)
- [ADR-006a](./99-adr/ADR-006a-kakao-notification.md)
- [ADR-006b](./99-adr/ADR-006b-kakao-retry-eta.md)

## [선택 5] 변경 로그

- _(작업 진행 시 기록)_
