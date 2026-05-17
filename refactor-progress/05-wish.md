# 05-wish · 위시 도메인

> **PR #13 (Phase A) + PR #14 (Phase B)**.
> 인라인 패턴 6위치 file:line 박제 — 이동 대상 명시.

---

## 1. 현재 상태 진단

- 패키지: `gift.wish`
- 대상 파일 — 인라인 패턴 6위치:

| # | 패턴 | 위치 | 이동 대상 |
|---|---|---|---|
| 1 | 인증 인라인 (getWishes) | `WishController.java:43-46` | `AuthenticationException`(401) + 글로벌 `@RestControllerAdvice` (01b 산출물 활용) |
| 2 | 인증 인라인 (addWish) | `WishController.java:57-60` | 同上 |
| 3 | 인증 인라인 (removeWish) | `WishController.java:84-87` | 同上 |
| 4 | product 존재 체크 | `WishController.java:63-66` | `ProductService.findByIdOrThrow` → `NotFoundException`(404) |
| 5 | 중복 wish 체크 | `WishController.java:69-72` | `WishService.add` 내부 (현재 동작 보존 — 200 OK 반환) |
| 6 | 소유권 체크 | `WishController.java:95-97` | `WishService.remove` 내부 → `AuthorizationException`(403) |

- 외부 의존: 04.5 (`Wish.member: Member` 사용 가능), 01b (글로벌 `@RestControllerAdvice`)

---

## 2. 목표 산출물 + Phase A/B 체크리스트

### Phase A (PR #13) — 구조 변경

- [x] A.1 `gift.wish.WishService` 신설 — `list(Member, Pageable)` / `add(Member, Long productId)` -> `AddOutcome(wish, newlyCreated)` / `remove(Member, Long wishId)` -> `RemoveOutcome` enum (`DELETED / NOT_FOUND / FORBIDDEN`) / `removeByMemberAndProduct(Member, Long productId)` -> boolean. `WishController` 가 Repository 직접 의존 없이 위임. 인증 인라인 (3회) 은 Phase B 까지 유지.

### Phase B (PR #14) — 작동 변경

- [x] B.1 인증 인라인 3회 제거 — `WishController` 가 `authenticationResolver.extractMemberOrThrow` 호출, 글로벌 `@RestControllerAdvice` 가 `AuthenticationException`(401) 처리.
- [x] B.2 product 존재 체크 → `WishService.add` 내부에서 `productRepository.findById` + `NotFoundException`(404). (ProductService 위임 대신 service 내부 직접 처리 — repository 의존이 이미 있어 indirection 불필요.)
- [x] B.3 중복 wish 체크 → `WishService.add` 내부 (Phase A 에서 이미 service 로 이동), 200 OK 동작 보존 — `AddOutcome.newlyCreated` 가 controller 분기 정보 제공.
- [x] B.4 소유권 체크 → `WishService.remove` 내부, `AuthorizationException`(403) throw. `RemoveOutcome` enum 폐기 (예외 단일 패턴).
- [x] B.5 `WishService` 클래스 레벨 `@Transactional(readOnly=true)`, mutating `add` / `remove` / `removeByMemberAndProduct` 에 `@Transactional` 오버라이드.

---

## 3. 검증 명령

```
./gradlew test
```

---

## 4. 변경 로그

- 2026-05-17: Phase A 완료 — `WishService` 추출 (list / add / remove / removeByMemberAndProduct), `WishController` 위임. 컨트롤러는 인증 인라인 + 응답 코드 매핑만 보유. `WishServiceTest` 6건 추가 (add new/dup/unknown, remove owner/non-owner, list).
- 2026-05-17: Phase B 완료 — 인라인 5건 흡수. `@Transactional`, `NotFoundException`(404), `AuthorizationException`(403), `extractMemberOrThrow` 단일 진입, `RemoveOutcome` enum 폐기. `WishRepository.findByMember_IdAndProduct_Id` 에 `@EntityGraph(product)` 추가 (lazy 안전성). 회귀 보호: `WishServiceTest` 7건 (NotFound product + NotFound wish 신규), `WishControllerValidationTest` 신규 (401 글로벌 advice 경유).
