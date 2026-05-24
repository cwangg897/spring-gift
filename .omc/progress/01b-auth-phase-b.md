# PR #5 (01b-auth Phase B) 진행 추적

## 목표 (refactor-progress/01b-auth.md §Phase B)
- **B.1**: `KakaoAuthService.loginWithKakaoCode` 에 `@Transactional` (Member 생성+토큰 갱신 원자화)
- **B.2**: `KakaoLoginException extends DomainException`(422) 신설, RestClient 예외 wrap
- **B.3**: `AuthenticationResolver` 정련 — null 반환 대신 `AuthenticationException`(401) throw 메서드 추가
- **B.4**: `gift.support.GlobalExceptionHandler` 강화 — `DomainException` 계층 통합 매핑 (ADR-007 본 도입)

## 단계별 진행

- [x] 1~9 모두 완료. `./gradlew test` 16/0/0
- [x] 10. doc 갱신
- [x] 11. Architect ARCHITECT-APPROVE. 잔여 권고 5건 (RestClient timeout, @Deprecated 마커, fallback 핸들러, 422 vs 502, tx 중첩 javadoc) 모두 후속.
- [x] 12. 커밋 분리: `72bfd84 feat(support)` + `129be1d feat(auth)` + `(docs)`
- [x] 13. `/oh-my-claudecode:cancel` (진행 중)

## Phase B 원칙 (작동 변경, 증거 필요)
- `KakaoLoginException` 응답 코드 의도된 422 (이전: 외부 RestClient 예외 그대로 500)
- 기존 `MemberControllerLoginTest.wrongPasswordReturns401` 회귀 없음 (DomainException 계층 변경에도 401 보존)
- `AuthenticationResolver.extractMemberOrThrow` 만 추가, 기존 `extractMember(null 반환)`는 호출처가 정리될 때까지 유지

## 비범위 (다음 도메인 Phase)
- 기존 `extractMember` 호출처 (`WishController`, `OrderController`) 변경 — 05-wish, 06-order Phase B에서 일괄 정리
