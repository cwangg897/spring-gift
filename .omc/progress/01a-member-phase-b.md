# PR #3 (01a-member Phase B) 진행 추적

> 토큰 부족 / 세션 중단 시 이 파일을 보고 다음 세션에서 이어 작업.

## 목표 (refactor-progress/01a-member.md §Phase B)
- **B.1**: `MemberService` mutating 메서드 `@Transactional`, 조회 `readOnly=true`
- **B.2**: `Member.matchesPassword(String raw)` 도메인 메서드 추가, 컨트롤러/서비스의 `equals` 비교 제거
- **B.3**: 인증 실패 → `AuthenticationException`(401) + 글로벌 `@RestControllerAdvice` (ADR-007 부분 도입)

## 단계별 진행

- [x] 1. `gift/support/exception/AuthenticationException.java` 신설 (401)
- [x] 2. `gift/support/GlobalExceptionHandler.java` (`@RestControllerAdvice`) 신설
- [x] 3. `Member.matchesPassword(String raw)` 도메인 메서드 추가
- [x] 4. `MemberService`: `@Transactional` + `readOnly`, `matchesPassword` 호출, `AuthenticationException` throw
- [x] 5. `MemberServiceTest` 갱신 + `MemberControllerLoginTest` 신설 (401 응답 검증)
- [x] 6. `./gradlew test` 9/0/0 그린 (19초)
- [x] 7. `refactor-progress/01a-member.md` Phase B 체크박스 + 변경 로그
- [x] 8. `refactor-progress/README.md` 진행률 표 PR #3 갱신
- [x] 9. Architect 검증 — ARCHITECT-APPROVE (잔여 권고 모두 후속 phase)
- [x] 10. 커밋 분리: `9968e4f feat(support)` + `21946d4 feat(member)`
- [x] 11. `/oh-my-claudecode:cancel` (진행 중)

## Phase B 원칙 (작동 변경, 증거 필요)
- 응답코드 401 (이전 400) 의도된 변경 — 테스트로 증명
- 트랜잭션 부착으로 register/authenticate가 원자적 (실패 시 부분 저장 방지)
- `matchesPassword` 도메인 캡슐화

## 비범위
- MemberController 의 `IllegalArgumentException` 핸들러 (중복 이메일 → 400) — Phase B에서 유지. 글로벌 advice 가 `AuthenticationException` 만 처리. 다른 도메인 예외 정리는 각 도메인 Phase B 에서.

## 환경 메모
- Java 21 launcher 필수
- Docker Desktop 가동 필요
