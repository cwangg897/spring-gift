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

- [ ] A.1 `gift.wish.WishService` 신설 — `list(Member, Pageable)` / `add(Member, Long productId)` / `remove(Member, Long wishId)` / **`removeByMemberAndProduct(Member, Long productId)`** (06-order Phase B 에서 호출)

### Phase B (PR #14) — 작동 변경

- [ ] B.1 인증 인라인 3회 제거 (#1, #2, #3) → 글로벌 advice 가 처리
- [ ] B.2 product 존재 체크(#4) → ProductService / `NotFoundException`(404)
- [ ] B.3 중복 wish 체크(#5) → `WishService.add` 내부, 현재 동작(200 OK) 보존
- [ ] B.4 소유권 체크(#6) → `WishService.remove` 내부, `AuthorizationException`(403)
- [ ] B.5 `WishService` mutating `@Transactional`, 조회 `readOnly=true`

---

## 3. 검증 명령

```
./gradlew test
```

---

## 4. 변경 로그

- _(작업 진행 시 기록)_
