# 01a-member · 회원 도메인

> **PR #2 (Phase A) + PR #3 (Phase B)**. 묶음: 01b-auth 와 함께 product 진입 전 종료.

---

## 1. 현재 상태 진단

- 패키지: `gift.member`
- 대상 파일:
  - `MemberController.java:33-42` — `register`: 중복 체크/save/토큰 발급 인라인
  - `MemberController.java:44-55` — `login`: 평문 비밀번호 `equals` 비교, 토큰 발급 컨트롤러
  - `MemberController.java:57-60` — 도메인별 `@ExceptionHandler` (ADR-007 통합 대상)
  - `AdminMemberController.java` — Thymeleaf, repository 직접 호출 (line 30, 45, 50, 82, 89)
  - `Member.java:49-65` — `chargePoint`, `deductPoint` **이미 엔티티 존재** (유지)
- 외부 의존: `gift.auth.JwtProvider` (01b-auth 와 결합)

---

## 2. 목표 산출물 + Phase A/B 체크리스트

### Phase A — 구조 변경

- [x] A.1 `gift.member.MemberService` 신설 — `register(MemberRequest)` / `authenticate(MemberRequest)` / `findById(Long)` / `existsByEmail(String)` + admin 메서드 (`findAll`, `createForAdmin`, `update`, `chargePoint`, `delete`). `MemberController` 위임. ✓
- [x] A.2 `AdminMemberController` 도 `MemberService` 재사용. `MemberRepository` 직접 호출 0건. ✓

### Phase B — 작동 변경

- [ ] B.1 `MemberService` mutating 메서드 `@Transactional`, 조회 `readOnly=true`
- [ ] B.2 `Member.matchesPassword(String raw)` 도메인 메서드 추가, 컨트롤러의 `equals` 제거
- [ ] B.3 인증 실패 → `AuthenticationException`(401) + 글로벌 `@RestControllerAdvice` (ADR-007)

---

## 3. 검증 명령

```
./gradlew test
```

머지 게이트: 전체 그린 + README 진행률 갱신 + §4 변경 로그.

---

## 4. 변경 로그

- 2026-05-16: Phase A 완료 — `MemberService` 추출, 컨트롤러 위임. `MemberServiceTest` 4건 (register/duplicate/authenticate/wrong password) + 기존 3건 = `./gradlew test` 7/0/0 그린. 부수: `AbstractIntegrationTest` 를 `static { MYSQL.start(); }` + `@DynamicPropertySource` 패턴으로 교체 (`@Container` annotation 이 클래스 간 lifecycle 충돌로 두 번째 Testcontainer 사용 클래스에서 ConnectException 발생하던 회귀 수정).
