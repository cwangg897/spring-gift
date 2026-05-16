# spring-gift Refactor Progress

> **Source of truth**: `/prd.md`
> **Plan version**: v3 (RALPLAN consensus — Planner ✓ Architect ✓ Critic ✓)
> **Status**: pending approval → execution

## 채택된 전략: 옵션 C (하이브리드)
도메인 단위 직렬 진행 + 트랜잭션 경계 우선화 횡단 단계 + JPA 매핑 통일 횡단 단계.

## 15 PR 시퀀스 (분모 박제)

| # | ID | 단계 | 종류 | PR 상태 | 비고 |
|---|---|---|---|---|---|
| 1 | 00 | test-infra | 단일 | [ ] | Testcontainers + Flyway 부트업 |
| 2 | 01a | member Phase A | 도메인 | [ ] | MemberService 가드레일 |
| 3 | 01a | member Phase B | 도메인 | [ ] | 도메인 불변식 정리 |
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

**분모: 15 PR**. 흔들리지 않음.

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

## 진행 체크 규약

### PR 단위
- 1 PR = 1 도메인 × 1 Phase (A 또는 B) 또는 1 횡단 단계
- "한 조각"은 PR **내부 커밋 단위**로 해석 (prd 라인 32 "다음 변경 1개")

### 셀프 머지 게이트 (V3-C)
PR description에 다음이 모두 없으면 머지 금지:
1. 체크박스 3요소 캡처 (코드/위임/테스트)
2. 본 README의 진행률 표 갱신 (`[ ]` → `[x]`)
3. 해당 도메인 문서의 "변경 로그" 1줄 추가

### 0단계 일반 규칙 (V3-3)
> 0단계는 **직전 PR 산출물 그린 확인**이다. 자기 PR이 만든 테스트는 0단계가 될 수 없다 (자가참조 금지). 직전 PR이 없는 경우(00-test-infra)는 별도 3요소(00 문서 참조).

### 체크박스 3요소 (V3-1, 통일 기준)
모든 작업 체크박스는 다음 3요소가 모두 충족될 때만 ✓:
- **(a) 코드**: 명시된 클래스/메서드가 지정 위치에 존재
- **(b) 위임**: 기존 호출자가 새 위치 호출 + 폐기 시 `grep` 결과 0건
- **(c) 테스트**: `./gradlew test --tests "gift.<domain>.*"` 그린 + PR description에 SHA + 로그 캡처

## 통일 문서 구조

모든 도메인 문서는 [`_template.md`](./_template.md)의 **필수 4 + 선택 5** 섹션 구조를 따른다.

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
| ADR-007 | 동시성 제약 사항 | 박제 (범위 외) |

## 비범위 (out of scope)

- `src/main/kotlin/` (현재 비어있음)
- Thymeleaf admin → REST 전환 (`AdminMemberController`, `AdminProductController`)
- BCrypt/Argon2 등 비밀번호 해싱 도입
- 카카오 알림 재시도/DLQ (ADR-006b로 분리)
- 동시성 제어(낙관락/비관락) (ADR-007로 분리)
- Gradle Kotlin DSL의 kotlin plugin 정리

## 변경 로그

- 2026-05-16: v3 plan consensus 도달 (Planner ✓ Architect ✓ Critic ✓), 문서 시스템 초기화
