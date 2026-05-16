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

- [ ] A.1 `gift.auth.KakaoAuthService` 신설 — `loginWithKakaoCode(code): TokenResponse`. 컨트롤러 위임.
- [ ] A.2 Member upsert 를 `MemberService.findOrCreateByKakao(email)` 로 위임

### Phase B — 작동 변경

- [ ] B.1 `KakaoAuthService.loginWithKakaoCode` 에 `@Transactional` (Member 생성+토큰 갱신 원자화)
- [ ] B.2 `KakaoLoginException extends DomainException` (422) 신설, RestClient 예외 wrap
- [ ] B.3 `AuthenticationResolver` 정련 — null 반환 대신 `AuthenticationException`(401) throw
- [ ] B.4 `gift.support.GlobalExceptionHandler` (`@RestControllerAdvice`) 신설 — 글로벌 예외 처리 진입점 (이후 모든 도메인 활용)

---

## 3. 검증 명령

```
./gradlew test
```

---

## 4. 변경 로그

- _(작업 진행 시 기록)_
