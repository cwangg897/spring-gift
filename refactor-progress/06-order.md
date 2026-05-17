# 06-order · 주문 도메인 (최종 PR, A+B 합본)

> **PR #15**. Phase A 의 `OrderFacade` 승격 + 폐기와 Phase B 의 누락 동작 + 이벤트화 가 같은 PR 안에서 진행 (단일 PR 내 커밋 단위로 분리).

---

## 1. 현재 상태 진단

- 패키지: `gift.order`
- 대상 파일:
  - `OrderFacade.java` (01.5 산출물, `@Deprecated(forRemoval=true)`) — Phase A 에서 service 로 승격 후 제거
  - `OrderController.java:69-102` — 01.5 이후 facade 위임 상태. service 승격 시 의존 클린업
  - `OrderController.java:104-113` — `sendKakaoMessageIfPossible`: 01.5 에서 WARN 로깅으로 변경됨. Phase B 에서 이벤트화
  - `KakaoMessageClient.java:16-29` — 외부 HTTP, 본 사이클 동기 유지 (비동기는 ADR-006b 후속)
  - **누락 동작**: `OrderController.java:67-68` 주석만 있고 wish cleanup 호출 없음 — 05-wish 의 `removeByMemberAndProduct` 호출 필요

---

## 2. 목표 산출물 + 체크리스트

### Phase A (커밋 시리즈) — facade 승격 + 폐기

- [x] A.1 `OrderService` 신설 — `placeOrder` (구 `OrderFacade.createOrder` 본체 + 이벤트 발행 추가), `findByMember`. `@Transactional(readOnly=true)` 클래스 레벨 + `@Transactional` 오버라이드.
- [x] A.2 `OrderController` 가 `OrderService` + `AuthenticationResolver` 만 주입 (의존 6 → 2). `OrderRepository` 직접 의존 제거. GET 도 service 위임.
- [x] A.3 `OrderFacade.java` 삭제. `grep -rn 'OrderFacade' src/` 결과 0 건 확인 완료.

### Phase B (커밋 시리즈) — 누락 동작 + 이벤트화

- [x] B.1 위시 정리 누락 동작 구현 — `OrderService.placeOrder` 가 주문 저장 후 `wishService.removeByMemberAndProduct(member, product.getId())` 호출 (boolean 반환은 무시).
- [x] B.2 카카오 알림 이벤트화 — `OrderCompletedEvent` record 발행, `KakaoNotificationListener` 가 `@TransactionalEventListener(phase=AFTER_COMMIT)` 로 처리. 실패 시 `log.warn`, `@Transactional` 미부착 (ADR-006a §2-b/c).
- [x] B.3 `KakaoMessageClient.sendToMe` 시그니처를 `(OrderCompletedEvent)` 로 단일화 — listener 가 단일 진입점. 컨트롤러의 try/catch 잔재는 facade 폐기로 자연 소멸.

### 후순위 (ADR-006b)

- [ ] B.4 (결정만 박제, 구현은 후속 사이클) `@Async("kakaoNotificationExecutor")` + `ThreadPoolTaskExecutor`

---

## 3. 검증 명령

```
./gradlew test
grep -rn "OrderFacade" src/   # 반드시 0 건
```

### 카카오 알림 테스트 (ADR-006a 함정 회피)

`AFTER_COMMIT` 이벤트 발화 검증 테스트는:
- `@Transactional` 미부착 (테스트 트랜잭션 롤백 시 이벤트 미발화)
- `@Sql(scripts="...cleanup.sql", executionPhase=AFTER_TEST_METHOD)` cleanup
- `@RecordApplicationEvents` 로 이벤트 캡처

시나리오:
1. 주문 정상 커밋 → 이벤트 발화 + Kakao 호출
2. 주문 트랜잭션 롤백 → 이벤트 미발화
3. 주문 커밋, Kakao API 실패 → 주문 정상 응답 + WARN 로그

---

## 4. 변경 로그

- 2026-05-17: PR #15 완료 (Phase A+B 합본 + 시퀀스 종결). `OrderService` 승격 + `OrderFacade` 폐기 + 위시 정리 동작 추가 + `OrderCompletedEvent` / `KakaoNotificationListener` AFTER_COMMIT 이벤트화. `OrderServiceIntegrationTest` 4건 (정상 / 롤백 / option not found / wish cleanup). 15/15 시퀀스 종결.
