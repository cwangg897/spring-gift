# PR #2 (01a-member Phase A) 진행 추적

> 토큰 부족 / 세션 중단 시 이 파일을 보고 다음 세션에서 이어 작업.

## 목표 (refactor-progress/01a-member.md §Phase A)
- **A.1**: `gift.member.MemberService` 신설 — `register(MemberRequest)` / `authenticate(MemberRequest)` / `findById(Long)` / `existsByEmail(String)`, 컨트롤러 위임
- **A.2**: `AdminMemberController` 도 `MemberService` 재사용

## 단계별 진행 (이어 작업할 때 첫 미체크 항목부터)

- [x] 1. 기존 파일 재확인 (`MemberController.java`, `AdminMemberController.java`, `MemberRepository.java`)
- [x] 2. `gift/member/MemberService.java` 신설 (register/authenticate/findById/existsByEmail + admin 메서드)
- [x] 3. `MemberController.java` 수정 → service 위임 (비즈니스 로직 제거)
- [x] 4. `AdminMemberController.java` 수정 → service 재사용
- [x] 5. `MemberServiceTest` 추가 (register/authenticate happy path 검증)
- [x] 6. `./gradlew test` 전체 그린 (7/0/0). 부수: AbstractIntegrationTest 를 `static { MYSQL.start(); }` + `@DynamicPropertySource` 패턴으로 교체 (Testcontainer 클래스 간 lifecycle 충돌 해결).
- [x] 7. `refactor-progress/01a-member.md` A.1/A.2 체크박스 + 변경 로그 갱신
- [x] 8. `refactor-progress/README.md` 진행률 표 PR #2 `[ ]` → `[x]`
- [x] 9. Architect 검증 — ARCHITECT-APPROVE. 잔여 권고: ADR-001 doc amend (반영 완료).
- [x] 10. 커밋 분리: `c93621e fix(test-infra)` + `016593f feat(member)` (사용자 짧은 커밋 원칙)
- [x] 11. `/oh-my-claudecode:cancel` (다음 단계)

## Phase A 원칙 (회귀 방지)
- **작동 동일** (prd 라인 6): 메서드 시그니처/응답코드/예외 메시지 변경 0건
- `@Transactional` 부여는 Phase B (PR #3) — 본 PR 에서는 service 클래스 + 위임만
- 비즈니스 로직 (중복 체크, 비밀번호 비교, 토큰 발급) 그대로 이동, 변경 0

## 환경 메모
- Java 21 launcher 필수: `JAVA_HOME=/Users/choewang-gyu/Library/Java/JavaVirtualMachines/corretto-21.0.9/Contents/Home`
- Docker Desktop 가동 필요 (FlywayMigrationIntegrationTest)

## 상태
- 작업 시작: 2026-05-16
- 마지막 갱신: _작업 진행 중 갱신_
