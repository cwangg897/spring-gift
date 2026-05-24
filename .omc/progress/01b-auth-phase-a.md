# PR #4 (01b-auth Phase A) 진행 추적

## 목표 (refactor-progress/01b-auth.md §Phase A)
- **A.1**: `gift.auth.KakaoAuthService` 신설 — `loginWithKakaoCode(code): TokenResponse` (KakaoLoginClient + MemberService + JwtProvider 조합), 컨트롤러 위임
- **A.2**: Member upsert 를 `MemberService.findOrCreateByKakao(email, accessToken)` 로 위임

## 단계별 진행

- [x] 1. `MemberService.findOrCreateByKakao(email, accessToken)` 추가 (`@Transactional`)
- [x] 2. `gift.auth.KakaoAuthService` 신설 (`loginWithKakaoCode`, `buildLoginUrl`)
- [x] 3. `KakaoAuthController` 위임으로 단순화 — 4 의존 → 1 (`KakaoAuthService`)
- [x] 4. `KakaoAuthServiceTest` 3건 (URL/신규/기존 갱신)
- [x] 5. `./gradlew test` 12/0/0 (14초)
- [x] 6. refactor-progress 문서 갱신
- [x] 7. Architect 검증 — ARCHITECT-APPROVE
- [x] 8. 커밋 분리: `d189198 feat(member)` + `b8cd0d1 feat(auth)`
- [x] 9. `/oh-my-claudecode:cancel` (진행 중)

## Phase A 원칙 (작동 동일)
- 비즈니스 로직 변경 0 — 모든 호출 순서 보존
- 응답코드 유지 (302 login redirect, 200 callback)
- KakaoAuthController 가 `MemberRepository` / `JwtProvider` / `KakaoLoginClient` 를 직접 호출 안 함

## 비범위 (Phase B)
- `@Transactional` 정합 (findOrCreateByKakao 는 이미 부여)
- 도메인 예외 (`KakaoLoginException` 등)
- `AuthenticationResolver` 정련
