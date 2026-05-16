# 05-wish · 위시 도메인 (V3-6 file:line 박제)

> **PR #13 (Phase A) + PR #14 (Phase B) / 15**.
> **V3-6 박제**: `WishController.java` 인라인 패턴 6위치 file:line 명시.

---

## [필수 1] 0단계 그린 캡처

- 직전 PR: `04.5-fk-unification` (PR #12)
- 그린 캡처: JPA 매핑 통일 + N+1 회귀 없음 검증 통과 — 커밋 SHA 첨부

---

## [필수 2] 현재 상태 진단 (V3-6 file:line 박제)

- 패키지: `gift.wish`
- 대상 파일 — **인라인 패턴 6위치**:

| # | 패턴 | 위치 (file:line) | 이동 대상 |
|---|---|---|---|
| 1 | 인증 인라인 (getWishes) | `WishController.java:43-46` | `AuthenticationException`(401) + `@RestControllerAdvice` 통합 (ADR-007). 또는 `AuthenticationResolver` 가 throw 하도록 정련 |
| 2 | 인증 인라인 (addWish) | `WishController.java:57-60` | 同上 (동일 패턴 3회 중복 제거) |
| 3 | 인증 인라인 (removeWish) | `WishController.java:84-87` | 同上 |
| 4 | product 존재 체크 인라인 | `WishController.java:63-66` | `ProductService.findByIdOrThrow(Long)` → `NotFoundException`(404) (ADR-007 통합) |
| 5 | 중복 wish 체크 인라인 | `WishController.java:69-72` | `WishService.add` 내부 — `findByMemberIdAndProductId` 후 기존 wish 반환 또는 동일 동작 보존 |
| 6 | 소유권 체크 인라인 | `WishController.java:95-97` | `WishService.remove` 내부 — `wish.getMember() != currentMember` 시 `AuthorizationException`(403) |

- 외부 의존: 04.5 (JPA 매핑 통일 완료 — `Wish.member: Member` 사용 가능), 01b (AuthenticationException)

---

## [필수 3] 목표 산출물 + Phase A/B 체크리스트

### Phase A (PR #13) — 구조 변경 (작동 동일)

- [ ] **A.1** `gift.wish.WishService` 신설 — (a) `list(Member, Pageable)` / `add(Member, Long productId)` / `remove(Member, Long wishId)` / **`removeByMemberAndProduct(Member, Long productId)` (06-order Phase B에서 호출)** (b) `WishController` 위임만, `wishRepository`/`productRepository` 직접 호출 grep 결과 controller 에서 0건 (c) `WishServiceTest` + `WishControllerTest` (WebMvcTest) 그린

### Phase B (PR #14) — 작동 변경 (V3-6 6위치 정리)

- [ ] **B.1** **인증 인라인 3회 → 글로벌 처리로 단일화** — (a) `AuthenticationResolver` 가 실패 시 `AuthenticationException`(401) throw, `@RestControllerAdvice` 핸들러가 변환 (01b Phase B.3 기반) (b) `WishController.java:43-46, 57-60, 84-87` 의 if-null-401 패턴 제거, controller는 `@RequestHeader` + service 호출만 (c) 401 응답 통합 테스트
- [ ] **B.2** **product 존재 체크 → ProductService** — (a) `ProductService.findByIdOrThrow(Long): Product` 또는 `WishService.add` 내부에서 `productRepository.findById(...).orElseThrow(NotFoundException::new)` (b) `WishController.java:63-66` 의 if-null-404 제거 (c) 404 응답 통합 테스트
- [ ] **B.3** **중복 wish 체크 → WishService** — (a) `WishService.add` 내부에 중복 검사, 기존 wish 발견 시 동일 정책(200 OK 반환) 또는 정책 변경(409 Conflict) — **현재 동작 보존을 위해 200 OK 유지**, ADR 메모로 향후 409 후보 (b) `WishController.java:69-72` 인라인 제거 (c) 중복 추가 시 200/기존 wish 반환 통합 테스트
- [ ] **B.4** **소유권 체크 → WishService** — (a) `WishService.remove` 내부에 `wish.getMember().equals(currentMember)` 검사, 불일치 시 `AuthorizationException`(403) (b) `WishController.java:95-97` 인라인 제거 (c) 타인 wish 삭제 시도 → 403 통합 테스트
- [ ] **B.5** `WishService` 메서드에 `@Transactional` 부여 — (a) mutating `@Transactional`, 조회 `readOnly=true` (b) ADR-002 정합 (c) 회귀 보호 테스트
- [ ] **B.6** **`removeByMemberAndProduct` 메서드 추가 (06-order Phase B 의존)** — (a) `WishService.removeByMemberAndProduct(Member, Long productId)` (없으면 no-op) (b) — (신규) (c) 단위 테스트 (없을 때 no-op, 있을 때 삭제)

---

## [필수 4] 검증 명령 + README 게이트

```
./gradlew test --tests "gift.wish.*"
./gradlew test
```

게이트:
- [ ] PR #13, #14 머지 시 README 진행률 갱신

---

## [선택 1] 작동 변경 vs 구조 변경

| 변경 ID | 종류 | 관련 ADR |
|---|---|---|
| A.1 | 구조 | — |
| B.1, B.2 | 작동 (예외 통합) | ADR-007 |
| B.3 | 작동 (현재 동작 보존) | — |
| B.4 | 작동 (예외 통합) | ADR-007 |
| B.5 | 작동 (트랜잭션) | ADR-002 |
| B.6 | 작동 (누락 동작 준비) | — |

## [선택 3] 관련 ADR

- [ADR-002](./99-adr/ADR-002-tx-boundary.md)
- [ADR-007](./99-adr/ADR-007-concurrency.md) — 예외 계층

## [선택 5] 변경 로그

- _(작업 진행 시 기록)_
