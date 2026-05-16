# 01b-auth · 인증 도메인 (member 공동 작성자)

> **PR #4 (Phase A) + PR #5 (Phase B)**. 묶음: `KakaoAuthController` 가 Member 직접 생성하므로 01a 와 한 묶음.

---

## 1. 현재 상태 진단

- 패키지: `gift.auth`
- 대상 파일:
  - `KakaoAuthController.java:55-68` — callback 메서드가 토큰 교환 + 사용자 정보 + Member upsert + JWT 발급 4단계 인라인
  - `KakaoAuthController.java:61-64` — `MemberRepository` 직접 호출 (Member 신규 생성)
  - `AuthenticationResolver.java:25-33` — `MemberRepository` 직접 접근, 실패 시 null 반환 (예외 대신)
  - `JwtProvider.java:38-45,53-63` — JWT 발급/검증 OK (캡슐화 강화 대상)
  - `KakaoLoginClient.java` — 외부 HTTP, 예외 그대로 노출 가능
- 외부 의존: 카카오 OAuth, `gift.member.MemberService` (01a 산출물)

---

## 2. 목표 산출물 + Phase A/B 체크리스트

### Phase A — 구조 변경

- [x] A.1 `gift.auth.KakaoAuthService` 신설 — `loginWithKakaoCode(code): TokenResponse` + `buildLoginUrl(): String`. 컨트롤러 단일 의존. ✓
- [x] A.2 Member upsert 를 `MemberService.findOrCreateByKakao(email, accessToken)` 로 위임. `KakaoAuthController` 의 `MemberRepository`/`JwtProvider`/`KakaoLoginClient` 직접 호출 0건. ✓

### Phase B — 작동 변경

- [x] B.1 `KakaoAuthService.loginWithKakaoCode` 에 `@Transactional` ✓
- [x] B.2 `KakaoLoginException extends DomainException`(422) 신설, `KakaoLoginClient` 의 RestClient 예외 try/catch wrap ✓
- [x] B.3 `AuthenticationResolver.extractMemberOrThrow(authorization)` 추가 — 실패 시 `AuthenticationException`(401) throw (기존 `extractMember` 도 유지 — 5개 호출처는 도메인 Phase B 에서 일괄 정리) ✓
- [x] B.4 `GlobalExceptionHandler` 강화 — `DomainException` 단일 catch + `status()` 분기. `AuthenticationException` 도 `DomainException` 산하로 마이그레이션. ADR-007 본 도입. ✓

---

## 3. 검증 명령

```
./gradlew test
```

---

## 4. 변경 로그

- 2026-05-16: Phase A 완료 — `KakaoAuthService` 분리 (`loginWithKakaoCode`, `buildLoginUrl`), `MemberService.findOrCreateByKakao(email, accessToken)` 추가. `KakaoAuthController` 가 service 한 개만 의존. `KakaoAuthServiceTest` 3건 신규. `./gradlew test` 12/0/0 그린.
- 2026-05-16: Phase B 완료 — `DomainException` 추상 계층 + `Authentication/Authorization/NotFound/Duplicate` 4종 도메인 예외 신설. `KakaoLoginException`(422) + `KakaoLoginClient` 의 `RestClient` 예외 wrap. `KakaoAuthService.loginWithKakaoCode` `@Transactional`. `AuthenticationResolver.extractMemberOrThrow` 추가. `GlobalExceptionHandler` 단일 `DomainException` 분기 처리. `./gradlew test` 16/0/0.
