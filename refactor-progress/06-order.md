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

- [ ] A.1 `gift.order.OrderService` 신설 — `OrderFacade.createOrder` 본체 이전, `@Transactional` 유지, 책임 명확화
- [ ] A.2 의존 클린업 — `OrderService` 가 `MemberService` / `OptionService` (엔티티 메서드) / `WishService.removeByMemberAndProduct` 사용, 컨트롤러는 service 만 주입 (현재 6 빈 → 1 빈)
- [ ] A.3 **`OrderFacade` 폐기**: 파일 삭제, `grep -rn "OrderFacade" src/` 결과 0 건 확인, 별도 폐기 커밋

### Phase B (커밋 시리즈) — 누락 동작 + 이벤트화

- [ ] B.1 **위시 정리 누락 동작 구현** (prd 4.3): `OrderService.placeOrder` 가 `wishService.removeByMemberAndProduct(member, option.getProduct().getId())` 호출
- [ ] B.2 **카카오 알림 이벤트화 (ADR-006a)**: `OrderCompletedEvent` publish + `KakaoNotificationListener` (`@TransactionalEventListener(AFTER_COMMIT)`), 실패 시 WARN 로깅 only
- [ ] B.3 `KakaoMessageClient` 호출자 정리 — try/catch 위치 listener 로, controller 의 `catch (Exception ignored)` 잔재 제거

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

- _(작업 진행 시 기록)_
