# spring-gift Refactor Progress

> **Source of truth**: `/prd.md`
> **Plan version**: v3 (RALPLAN consensus — Planner ✓ Architect ✓ Critic ✓)
> **단순화 v3.1 (2026-05-16)**: TDD Red-Green-Refactor 의례, 0단계 동작 스냅샷, 3요소 체크박스 박제 제거. **단일 검증 기준 = 변경 후 `./gradlew test` 전체 그린** (prd 라인 30).

## 채택된 전략: 옵션 C (하이브리드)
도메인 단위 직렬 진행 + 트랜잭션 경계 우선화 횡단 단계 + JPA 매핑 통일 횡단 단계.

## 15 PR 시퀀스 (분모 박제)

| # | ID | 단계 | 종류 | PR 상태 | 비고 |
|---|---|---|---|---|---|
| 1 | 00 | test-infra | 단일 | [x] | Testcontainers + Flyway 부트업 ✓ |
| 2 | 01a | member Phase A | 도메인 | [x] | MemberService 가드레일 ✓ |
| 3 | 01a | member Phase B | 도메인 | [x] | 도메인 불변식 + 401 예외 통합 ✓ |
| 4 | 01b | auth Phase A | 도메인 | [ ] | KakaoAuthService 분리 |
| 5 | 01b | auth Phase B | 도메인 | [ ] | JwtTokenProvider 캡슐화 |
| 6 | 01.5 | tx-boundary | 횡단 | [ ] | OrderFacade(@Transactional) 임시 도입 |
| 7 | 02 | product Phase A | 도메인 | [ ] | ProductService 가드레일 |
| 8 | 02+03B | product+category Phase B | 도메인 | [ ] | category Phase B 흡수 |
| 9 | 03 | category Phase A | 도메인 | [ ] | CategoryService 가드레일 |
| 10 | 04 | option Phase A | 도메인 | [ ] | OptionService 가드레일 |
| 11 | 04 | option Phase B | 도메인 | [ ] | 이름검증/마지막옵션 규칙 이동 |
| 12 | 04.5 | fk-unification | 횡단 | [ ] | Wish/Order JPA 매핑 통일 |
| 13 | 05 | wish Phase A | 도메인 | [ ] | WishService 가드레일 |
| 14 | 05 | wish Phase B | 도메인 | [ ] | 인라인 6패턴 이동 |
| 15 | 06 | order Phase A+B | 도메인 | [ ] | OrderFacade 폐기 + 이벤트화 |

**분모: 15 PR**.

## 도메인 종속성 그래프

```
00-test-infra
     │
     ▼
01a-member ──► 01b-auth (auth는 member 공동 작성자)
     │
     ▼
01.5-tx-boundary (OrderFacade 임시)
     │
     ▼
02-product ──► 03-category (Phase B 흡수)
     │
     ▼
04-option
     │
     ▼
04.5-fk-unification (JPA 매핑만)
     │
     ▼
05-wish
     │
     ▼
06-order (OrderFacade → OrderService 승격 + 폐기)
```

## 검증 기준 (단순)

**유일한 게이트**: `./gradlew test` 전체 그린 (prd 라인 30 "최소 기준은 변경 후 전체 테스트 통과").

각 PR 머지 전:
1. 변경한 코드와 관련된 테스트가 (필요시 추가/수정되어) 통과
2. **전체 `./gradlew test` 그린**
3. README 진행률 표 `[ ]` → `[x]` 갱신 + 해당 도메인 문서 변경 로그 1줄 추가

## 통일 문서 구조

모든 도메인 문서는 [`_template.md`](./_template.md) 의 단순 4 섹션을 따른다:
1. 현재 상태 진단 (file:line 단위 결함)
2. 목표 산출물 + Phase A/B 체크리스트
3. 검증 명령 (`./gradlew test`)
4. 변경 로그

## ADR 인덱스

→ [`99-adr/README.md`](./99-adr/README.md)

| ADR | 주제 | 상태 |
|---|---|---|
| ADR-001 | Testcontainers + 격리정책 | 박제 |
| ADR-002 | 트랜잭션 경계 = 서비스 계층 | 박제 |
| ADR-003 | JPA FK 매핑(ManyToOne+LAZY) + N+1 정책 | 박제 |
| ADR-004 | 검증 책임 매트릭스 + anti-pattern ban | 박제 |
| ~~ADR-005~~ | (폐기, ADR-001에 흡수) | — |
| ADR-006a | 카카오 알림 실패 정책 + 테스트 함정 | 박제 |
| ADR-006b | 카카오 알림 재시도/DLQ ETA | 박제 (범위 외) |
| ADR-007 | 동시성 제약 사항 + 예외 계층 통합 | 박제 |

## 비범위 (out of scope)

- `src/main/kotlin/` (현재 비어있음)
- Thymeleaf admin → REST 전환
- BCrypt/Argon2 등 비밀번호 해싱 도입
- 카카오 알림 재시도/DLQ (ADR-006b)
- 동시성 제어 (ADR-007)

## 변경 로그

- 2026-05-16: v3 plan consensus 도달 (Planner ✓ Architect ✓ Critic ✓), 문서 시스템 초기화
- 2026-05-16: PR #1 (00-test-infra) 완료. Testcontainers MySQL 8 + Singleton + `@ServiceConnection`. `./gradlew test` 3 그린.
- 2026-05-16: 단순화 v3.1 적용 — TDD Red-Green-Refactor 의례·0단계 동작 스냅샷·3요소 체크박스 박제 제거. 검증 기준은 "전체 테스트 그린" 1개.
- 2026-05-16: PR #2 (01a-member Phase A) 완료. `MemberService` 추출 + 컨트롤러 위임. `./gradlew test` 7/0/0. 부수 회귀 수정: `AbstractIntegrationTest` 컨테이너 lifecycle 패턴.
- 2026-05-16: PR #3 (01a-member Phase B) 완료. `@Transactional` 부착, `Member.matchesPassword`, `AuthenticationException`(401) + `GlobalExceptionHandler` 신설. `./gradlew test` 9/0/0.
