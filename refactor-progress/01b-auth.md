# 01b-auth · 인증 도메인 (member 공동 작성자)

> **PR #4 (Phase A) + PR #5 (Phase B) / 15**.
> **묶음 이유**: `KakaoAuthController.callback`이 `MemberRepository`로 Member를 직접 생성·저장 → auth는 member의 공동 작성자. 분리 도메인 불가.

---

## [필수 1] 0단계 그린 캡처

- 직전 PR: `01a-member Phase B` (PR #3)
- 그린 캡처: 01a 통합 테스트 + 전체 `./gradlew test` 메인에서 그린 — 커밋 SHA 첨부

---

## [필수 2] 현재 상태 진단

- 패키지: `gift.auth`
- 대상 파일:
  - `src/main/java/gift/auth/KakaoAuthController.java:55-68` — callback 메서드가 토큰 교환 + 사용자 정보 조회 + Member upsert + JWT 발급 4단계를 컨트롤러에서 인라인 조립
  - `src/main/java/gift/auth/KakaoAuthController.java:61-64` — `MemberRepository` 직접 호출 (Member 신규 생성·저장)
  - `src/main/java/gift/auth/AuthenticationResolver.java:25-33` — `MemberRepository` 직접 접근, 실패 시 null 반환 (예외 발생 대신 — ADR-007 통합 대상)
  - `src/main/java/gift/auth/JwtProvider.java:38-45,53-63` — JWT 발급/검증 OK (캡슐화 강화 대상)
  - `src/main/java/gift/auth/KakaoLoginClient.java` — 외부 HTTP 호출, 예외가 그대로 노출 가능 (도메인 예외로 매핑 필요)
- 외부 의존:
  - 카카오 OAuth (https://kauth.kakao.com, https://kapi.kakao.com)
  - `gift.member.MemberService` (01a 산출물)

---

## [필수 3] 목표 산출물 + Phase A/B 체크리스트

### Phase A — 구조 변경 (작동 동일)

- [ ] **A.1** `gift.auth.KakaoAuthService` 신설 — (a) `loginWithKakaoCode(String code): TokenResponse` 메서드, `KakaoLoginClient + MemberService + JwtProvider` 조합 (b) `KakaoAuthController` 에서 위임만, `MemberRepository` 직접 호출 grep 결과 0건 (c) `KakaoAuthServiceTest` (Mock client) + `KakaoAuthControllerTest` 그린
- [ ] **A.2** Member upsert를 `MemberService.findOrCreateByKakao(email)` 메서드로 위임 — (a) `MemberService` 에 메서드 추가 (b) `KakaoAuthService` 가 호출, controller 에서 `memberRepository.findByEmail`/`.save` 직접 호출 grep 결과 0건 (c) 통합 테스트로 신규 가입/재로그인 경로 그린

### Phase B — 작동 변경 (증거 필요)

- [ ] **B.1** Kakao 콜백 전체에 `@Transactional` 부여 — (a) `KakaoAuthService.loginWithKakaoCode` 에 `@Transactional`, Member 생성+토큰 갱신을 원자화 (b) ADR-002 정합 (c) 부분 실패 시 Member 미생성 검증 통합 테스트
- [ ] **B.2** `KakaoLoginClient` 예외 → 도메인 예외 매핑 — (a) `KakaoLoginException extends DomainException` (422) 신설, RestClient 예외 wrap (b) controller 까지 RestClient 예외 누출 grep 결과 0건 (c) Mock 실패 케이스 응답 422 검증
- [ ] **B.3** `AuthenticationResolver` 책임 정련 — (a) `extractMember` 가 null 반환 대신 `AuthenticationException`(401) throw, 호출처는 ADR-007 `@RestControllerAdvice` 가 변환 (b) `WishController.java:43-46, 57-60, 84-87`, `OrderController.java:54-56, 75-77` 의 인라인 401 패턴 제거 (05/06 PR에서 본격 반영) (c) 401 응답 통합 테스트

---

## [필수 4] 검증 명령 + README 게이트

```
./gradlew test --tests "gift.auth.*"
./gradlew test
```

게이트 (V3-C):
- [ ] PR #4, #5 머지 시 README 진행률 갱신

---

## [선택 1] 작동 변경 vs 구조 변경

| 변경 ID | 종류 | 관련 ADR |
|---|---|---|
| A.1, A.2 | 구조 | ADR-001 |
| B.1 | 작동 | ADR-002 |
| B.2, B.3 | 작동 | ADR-007 |

## [선택 3] 관련 ADR

- [ADR-002](./99-adr/ADR-002-tx-boundary.md)
- [ADR-007](./99-adr/ADR-007-concurrency.md) — 예외 계층 통합

## [선택 4] 후속 작업

- BCrypt/Argon2 비밀번호 해싱 (비범위, 후속)

## [선택 5] 변경 로그

- _(작업 진행 시 기록)_
