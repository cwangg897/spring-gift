# 01a-member · 회원 도메인

> **PR #2 (Phase A) + PR #3 (Phase B) / 15**.
> **묶음**: 01b-auth 와 한 묶음. product 진입 전 종료.

---

## [필수 1] 0단계 그린 캡처

- 직전 PR: `00-test-infra` (PR #1)
- 그린 캡처: V3-2 3요소(`build`, `contextLoads`, Testcontainers Flyway)가 main에서 그린 — 커밋 SHA 첨부

---

## [필수 2] 현재 상태 진단

- 패키지: `gift.member`
- 대상 파일:
  - `src/main/java/gift/member/MemberController.java:33-42` — `register`: 비즈니스 로직 인라인 (중복 체크, save, 토큰 발급)
  - `src/main/java/gift/member/MemberController.java:44-55` — `login`: 평문 비밀번호 비교(`equals`), 토큰 발급 컨트롤러에서
  - `src/main/java/gift/member/MemberController.java:57-60` — 도메인별 `@ExceptionHandler` (ADR-007 통합 대상)
  - `src/main/java/gift/member/AdminMemberController.java` — Thymeleaf, repository 직접 호출 (line 30, 45, 50, 82, 89) — Phase A에서 service 위임으로 정리
  - `src/main/java/gift/member/Member.java:57-65` — `deductPoint` **이미 엔티티 존재** (검증/유지)
  - `src/main/java/gift/member/Member.java:49-54` — `chargePoint` **이미 엔티티 존재** (검증/유지)
- 외부 의존:
  - `gift.auth.JwtProvider` (토큰 발급) — 01b-auth 와 결합

---

## [필수 3] 목표 산출물 + Phase A/B 체크리스트

### Phase A — 구조 변경 (작동 동일)

- [ ] **A.1** `gift.member.MemberService` 신설 — (a) `register(MemberRequest)` / `authenticate(MemberRequest)` / `findById(Long)` / `existsByEmail(String)` (b) `MemberController` 에서 위임만, `memberRepository` 직접 호출 grep 결과 controller에서 0건 (c) `MemberServiceTest` + 기존 동작 스냅샷(WebMvcTest `MemberControllerTest`) 그린
- [ ] **A.2** `AdminMemberController` 도 `MemberService` 재사용 — (a) `memberService.register/findById/chargePoint/delete` 위임 (b) `memberRepository.save/findById/deleteById` 직접 호출 grep 결과 admin controller에서 0건 (c) `./gradlew test --tests "gift.member.*"` 그린

### Phase B — 작동 변경 (증거 필요)

- [ ] **B.1** `@Transactional` 부여 — (a) `MemberService.register` 등 mutating 메서드에 `@Transactional`, 조회는 `@Transactional(readOnly=true)` (b) ADR-002 정합 (c) 통합 테스트로 중복 등록 시 데이터 보존 검증
- [ ] **B.2** `Member.matchesPassword(String raw)` 도메인 메서드 추가 — (a) `Member` 에 `matchesPassword(raw)` 메서드, 컨트롤러/서비스의 `equals` 비교 제거 (b) `MemberController.java:49-51` 제거, `MemberService.authenticate` 가 도메인 메서드 호출 (c) `MemberDomainTest` 그린
- [ ] **B.3** 인증 실패 응답 통일 — (a) `AuthenticationException`(401) 도메인 예외 신설 + `MemberService.authenticate` 가 throw (b) controller 자체 `@ExceptionHandler` 제거 (ADR-007 통합) (c) 401 응답 통합 테스트

> **검증/문서화 (V3-1 패턴)**: `Member.chargePoint`(49-54), `Member.deductPoint`(57-65)는 **이미 엔티티에 존재**하며 음수/잔액 가드 포함. 추가 이동 작업 없음. 본 도메인의 B 항목은 인증/패스워드 책임 정련만.

---

## [필수 4] 검증 명령 + README 게이트

```
./gradlew test --tests "gift.member.*"
./gradlew test
```

게이트 (V3-C):
- [ ] PR #2 (Phase A) 머지 시 README 진행률 갱신
- [ ] PR #3 (Phase B) 머지 시 README 진행률 갱신

---

## [선택 1] 작동 변경 vs 구조 변경

| 변경 ID | 종류 | 관련 ADR |
|---|---|---|
| A.1, A.2 | 구조 | ADR-001 (테스트), ADR-002 (예고) |
| B.1 | 작동 | ADR-002 |
| B.2, B.3 | 작동 | ADR-004 (검증 매트릭스), ADR-007 (예외 통합) |

## [선택 3] 관련 ADR

- [ADR-002](./99-adr/ADR-002-tx-boundary.md) — 트랜잭션 경계 = 서비스 계층
- [ADR-004](./99-adr/ADR-004-validation-matrix.md) — DTO/엔티티/서비스 검증 책임
- [ADR-007](./99-adr/ADR-007-concurrency.md) — 동시성 / 예외 통합

## [선택 5] 변경 로그

- _(작업 진행 시 기록)_
