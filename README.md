
## 1. 리팩토링 진행 방식

### 1.1 전략

- **도메인 단위 직렬 진행**: member → auth → product → category → option → fk-unification → wish → order.
- **횡단 단계 끼워넣기**: 도메인 직렬 진행 중간에 트랜잭션 경계 통일 (PR #6) 과 JPA FK 매핑 통일 (PR #12) 을 횡단으로 박았다.
- **Phase A / Phase B 분리**: 도메인마다 `Phase A = 구조 변경 (동작 동일)`, `Phase B = 작동 변경 (도메인 예외 + @Transactional + 검증 이동)` 으로 명확히 분리. 한 PR 에 두 가지 관심사를 섞지 않는다.

### 1.2 16 PR 시퀀스 (실제 진행 순서)

| # | 도메인 / 단계 | 핵심 산출물 |
|---|---|---|
| 1 | 00 test-infra | Testcontainers MySQL 8 + JVM-singleton + Flyway V1/V2 부트업 |
| 2 | 01a member Phase A | MemberService 추출 |
| 3 | 01a member Phase B | `@Transactional`, `Member.matchesPassword`, `AuthenticationException(401)` + 글로벌 advice |
| 4 | 01b auth Phase A | KakaoAuthService 분리 |
| 5 | 01b auth Phase B | `DomainException` 계층 + `KakaoLoginException(422)` + `extractMemberOrThrow` |
| 6 | 01.5 tx-boundary (횡단) | 임시 `OrderFacade @Transactional` 도입 → 주문 6단계 원자화 |
| 7 | 02 product Phase A | ProductService + CategoryService 추출 |
| 8 | 02+03B product+category Phase B | 엔티티 자가검증 (`ProductNameInvalidException`), `CategoryInUseException(409)`, 글로벌 advice 통합 |
| 9 | 03 category Phase A | PR #7/#8 흡수 박제 (docs 만) |
| 10 | 04 option Phase A | OptionService 추출 |
| 11 | 04 option Phase B | Option 자가검증, `LastOptionDeletionException(422)`, 가드 순서 재정렬 |
| 12 | 04.5 fk-unification (횡단) | Wish/Order 의 primitive `memberId` → `@ManyToOne(LAZY) Member` + `@EntityGraph` N+1 방지 |
| 13 | 05 wish Phase A | WishService 추출 (`AddOutcome` record + `RemoveOutcome` enum) |
| 14 | 05 wish Phase B | 인라인 5건 흡수 (인증/소유권/404), `RemoveOutcome` 폐기 |
| 15 | 06 order Phase A+B | OrderFacade 폐기 + OrderService 승격 + 위시 정리 + AFTER_COMMIT 이벤트화 |
| **16** | **후속 — Outbox** | **사용자가 지적한 "외부 API 실패 시 알림 유실" 시나리오 해결: outbox_event 테이블 + @Scheduled poller + MAX_ATTEMPTS=5 후 DEAD** |

### 1.3 검증 기준 (단 하나)

```
./gradlew test  # 전체 그린
```

prd 의 명시적 기준: "**최소 기준은 변경 후 전체 테스트 통과**".
TDD Red-Green-Refactor 사이클·동작 스냅샷 같은 의례는 일부러 도입하지 않았다.
PR 단위 회귀 테스트만 작성 + 전체 빌드 그린이면 머지.

### 1.4 ADR 박제

7 개 ADR 을 `refactor-progress/99-adr/` 에 박제. 각 ADR 은 결정·근거·대안·결과를 기록.

| ADR | 주제 |
|---|---|
| ADR-001 | Testcontainers 격리 정책 |
| ADR-002 | 트랜잭션 경계 = 서비스 계층 |
| ADR-003 | JPA `@ManyToOne(LAZY)` 통일 + N+1 정책 |
| ADR-004 | 검증 책임 매트릭스 (DTO=형식 / Entity=불변식 / Service=교차) |
| ADR-006a | 카카오 알림 실패 정책 (AFTER_COMMIT + WARN) |
| ADR-006b | 재시도/DLQ — **PR #16 에서 Outbox 옵션 활성화** |
| ADR-007 | 동시성 제약 + `DomainException` 계층 통합 |

진행률 표·도메인별 체크리스트·변경 로그는 `refactor-progress/README.md` 가 단일 출처.

---

## 2. AI 활용 방법

본 리팩토링은 Claude (Opus 4.7 / Sonnet 4.6) 와 **Ralph 워크플로** 로 진행했다.
모든 코드 변경은 다음 단계를 거쳤다.

### 2.1 단계별 워크플로

deslop 이란?
```
- 영어 slang "AI slop" (AI 가 만든 군더더기 코드) 을 줄여 부르는 말. AI 가 흔히 생성하는 죽은 코드 / 중복 / 불필요한 추상화 / 약한 테스트 / 안 쓰이는 wrapper 같은 걸 골라내서 지우는 검토 패스.
- 본 사이클에서는 ai-slop-cleaner 라는 OMC skill 이 매 PR 의 Architect APPROVE 직후 변경 파일에 한정해서 돌렸어요. 보통 결과는 "no changes needed" 였지만,
 PR #14 에서는 WishControllerValidationTest 의 메서드명이 실제 동작과 안 맞아서 (missingAuthHeaderReturns401 인데 실제로는 invalid token 보냄) 이름 정정한 사례 있음.

```

```
사용자 지시
    │
    ▼
[1] PRD 작성  ─ 작업을 User Story (US-001 ...) 단위로 쪼개고
    │           acceptance criteria 를 구체적·검증 가능하게 박제
    │           (예: "OptionService.findByIdOrThrow 가 NotFoundException throw")
    ▼
[2] 구현      ─ Claude 가 파일 편집 + 테스트 작성
    │
    ▼
[3] ./gradlew test  ─ 전체 그린만 검증 (TDD 사이클 없음)
    │
    ▼
[4] Architect 검증 ─ 별도 Claude agent 가 PRD criterion ↔ 실제 코드 file:line 매핑.
    │                APPROVE / REJECT. REJECT 시 [2] 구현 단계로 복귀해서 수정.
    │
    ▼
[5] Deslop 패스 ─ ai-slop-cleaner skill 로 변경 파일에 한정해서 군더더기 제거.
    │             ("AI slop" = AI 가 만든 죽은 코드 / 중복 / 불필요한 추상화 /
    │              약한 테스트 / 안 쓰이는 wrapper. 보통 no-op 으로 끝남)
    │
    ▼
[6] 회귀 재검증 ─ deslop 후 다시 그린
    │
    ▼
[7] 커밋 분리 + 푸시
```

### 2.2 사용자가 AI 에게 명시한 규칙 (memory 박제)

`~/.claude/projects/.../memory/` 에 영구 박제된 규칙:

- **Minimal process**: "TDD Red-Green-Refactor·동작 스냅샷·3요소 체크박스 같은 의례는 빼라. 변경 후 전체 테스트 통과 만 적용한다."
- **Short commits**: "한 커밋에 여러 관심사를 섞지 않고 작게 분리한다."
- 시스템이 `<tdd-mode>` 훅을 주입해도 사용자 지시가 우선.

### 2.3 AI 의 한계 / 사용자가 잡아낸 누락

AI 가 항상 옳지는 않았다. 본 사이클에서 사용자가 직접 잡아낸 누락 2건:

1. **PR #15 의 service-layer 박제 누락** — 06-order.md §A.2 가 "OrderService 가 OptionService / MemberService 위임" 을 박제했는데, AI 가 PR #15 에서 OptionRepository / MemberRepository 직접 의존으로 구현. 사용자가 "OptionService 없는데?" 로 지적 → follow-up PR 로 교정 (3 커밋).
2. **외부 API 실패 시 알림 유실** — AI 가 PR #15 에서 `@TransactionalEventListener(AFTER_COMMIT)` 만 사용. 카카오 API 실패 시 `log.warn` 만 남고 영구 손실. 사용자가 "모든 로직 성공했는데 외부 api 실패하는 경우는?" 로 지적 → Outbox 패턴 PR #16 추가 (5 커밋, 신규 테이블 + 스케줄러).

→ **PRD 와 ADR 을 미리 박제한 덕에 사용자가 plan vs implementation 불일치를 빠르게 검출** 할 수 있었다.

### 2.4 Reviewer Tier

Architect agent 는 변경 규모에 따라 모델을 다르게 썼다.

| 규모 | 모델 |
|---|---|
| <5 파일 / <100 라인 + 테스트 충분 | Sonnet (STANDARD) |
| 표준 변경 | Sonnet |
| >20 파일 또는 architectural 변경 | Opus (THOROUGH) |

**왜 규모마다 다르게 썼나:**

- **비용·시간 효율** — Opus 는 Sonnet 보다 호출당 토큰 비용·응답 시간이 크다. PR #7 처럼 ProductService 추출 같은 패턴-반복 PR 에 Opus 를 쓰면 낭비. 작은 변경 = 짧고 정확한 매핑이면 충분하므로 Sonnet.
- **검토 깊이가 필요한 변경만 Opus** — 변경 면적이 크거나 (PR #15: facade 폐기 + 이벤트화), 신규 스키마/트랜잭션 경계 재설계가 들어가면 (PR #16: outbox 테이블 + @Scheduled + AFTER_COMMIT 함정 회피) **다층 위험을 동시에 추론해야 한다**. Opus 의 긴 reasoning chain 이 OSIV 의존성·멱등성·payload 토큰 노출 같은 cross-cutting 이슈를 한 번에 잡아낸다.
- **Ralph skill 의 floor 규칙 준수** — Ralph 워크플로 자체에 "<5 files = STANDARD, >20 files or architectural = THOROUGH, 어떤 경우든 floor 는 STANDARD" 가 박제돼 있어 이를 따랐다.
- **결과** — Sonnet 으로 검증한 작은 PR 도 회귀 0건. Opus 가 검증한 큰 PR 에서는 비차단 권고 (예: PR #16 의 follow-up 4건) 를 정확히 짚어줘서 다음 사이클 후보로 박제 가능했다.

예: PR #15 (OrderFacade 폐기 + 이벤트화) 와 PR #16 (Outbox 패턴 + 신규 스키마) 는 Opus 로 검증.

### 2.5 Architect 의 비차단 권고 (후속 사이클 후보)

Outbox PR #16 에서 Opus architect 가 박제한 follow-up 4건:

1. ADR-006b "정확히-한-번" 표현 → 실제는 "최소-한-번" (멱등성 미보장) 으로 정정 필요.
2. `outbox_event.payload` 안에 평문 카카오 토큰 저장 → DB 덤프/로그 유출 위험. 토큰은 별도 조회로 분리 권고.
3. 테스트 프로파일에서 `@Scheduled` 비활성화 → 향후 outbox 테스트 격리.
4. "주문 롤백 시 outbox 행 미생성" 직접 회귀 테스트 1건 추가.

본 사이클 범위 외 — 다음 사이클 후보로 박제됨.

### 2.6 무엇이 효과적이었나
```
acceptance criteria = 그 작업이 "완료됐다" 고 인정받기 위한 구체적·검증 가능한 조건 목록. PRD 안에서 각 User Story (US-001, US-002 …) 가 가지는 체크리스트.
```

- **PRD 체크리스트를 file:line 수준으로 구체화** → architect 가 매핑하기 쉬워짐.
- **Phase A/B 분리** → 구조 변경 PR 이 작아져서 회귀 위험 감소.
- **ADR 선행 박제** → AI 가 patterns 일관성 유지 (예: ADR-007 `DomainException.status()` 패턴).
- **사용자 memory 박제 (minimal process / short commits)** → 의례 제거, 한 번 박제하면 이후 세션에서도 자동 적용.
- **별도 architect agent 검증** → 작성자(Claude)와 검토자(다른 Claude) 분리로 self-approval 회피.

### 2.7 무엇이 비효율적이었나
- **컨텍스트 압축 (compact) 후 정확도 저하** — 한 번 압축되면 다음 세션이 파일 재독해로 토큰 소비 증가.
- **Docker daemon off 시 Testcontainers 30초 timeout** — 사용자가 수동 시작 후 폴링.

---

## 3. 빌드 / 실행

```bash
# Java 21 + Docker Desktop 가동 필수
JAVA_HOME=/path/to/corretto-21 ./gradlew test
```
테스트 카운트: **46/0/0 그린**.

---

## 4. 참고

- `refactor-progress/README.md` — 15 PR 시퀀스 진행률 표 + 변경 로그
- `refactor-progress/99-adr/` — 7 개 ADR
- `refactor-progress/<domain>.md` — 도메인별 Phase A/B 체크리스트
- `prd.md` — 원본 미션 명세 (5주차)

## 배운점
매번 인텔리제이 터미널에서 작업하다 보니, 메모리에 저장하지 않고 종료되는 경우가 있었고, 이후 다시 프롬프트를 입력하면 AI가 이전 맥락을 잃어 일관성이 떨어지는 문제가 있었습니다. cmux를 통해 이런 문제를 어느 정도 방지할 수 있다는 점을 경험했습니다.

그리고 평소에는 예외를 하나의 클래스로 만들어 사용했는데, 이번에 AI가 구현한 코드를 보니 DomainException을 두고, 각 도메인에서 발생하는 예외가 이를 상속하도록 구성되어 있었습니다. 예외는 결국 개발자가 문제를 더 쉽게 파악하기 위해 작성하는 것이기 때문에, 이런 방식이 문제가 발생했을 때 원인을 찾기 더 쉽겠다고 생각했습니다.

다만 아쉬웠던 점도 있었습니다. 아직까지 AI는 동시성 이슈를 명확하게 정의해주지 않으면 해당 부분에 취약한 모습을 보였습니다. 또한 외부 API 연동 과정에서 내부 DB와 외부 API의 상태를 일치시키는 정합성 문제도, 명확하게 요구사항을 정의하지 않으면 완성도가 떨어지는 것 같았습니다.

이번 과제를 진행하면서 리팩토링에 대해 새롭게 알게 된 점은, 테스트 코드의 중요성입니다. 최근 Java에서 Kotlin으로 전환하는 강의를 보았는데, 이 경우에도 테스트 코드를 먼저 작성한 뒤 Kotlin으로 점진적으로 변환하더라고요. 사내에도 레거시 코드가 많기 때문에, 이런 방식을 활용해 레거시 리팩토링을 진행해보려고 합니다.
