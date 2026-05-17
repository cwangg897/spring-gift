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
| 4 | 01b | auth Phase A | 도메인 | [x] | KakaoAuthService 분리 ✓ |
| 5 | 01b | auth Phase B | 도메인 | [x] | DomainException 계층 + Kakao 422 + @Transactional ✓ |
| 6 | 01.5 | tx-boundary | 횡단 | [x] | OrderFacade(@Transactional) 임시 도입 ✓ |
| 7 | 02 | product Phase A | 도메인 | [x] | ProductService + CategoryService 가드레일 ✓ |
| 8 | 02+03B | product+category Phase B | 도메인 | [x] | category Phase B 흡수, 검증 이동·참조 검사·글로벌 advice 통합 ✓ |
| 9 | 03 | category Phase A | 도메인 | [x] | PR #7(`2bd6a11`)에 선행 흡수, PR #8(`548956a`)에 Phase B 흡수 — 박제만 처리 |
| 10 | 04 | option Phase A | 도메인 | [x] | OptionService 추출 + 컨트롤러 위임 ✓ |
| 11 | 04 | option Phase B | 도메인 | [x] | 엔티티 자가검증 + 도메인 예외 + 글로벌 advice 통합 ✓ |
| 12 | 04.5 | fk-unification | 횡단 | [x] | Wish/Order @ManyToOne(LAZY) 통일 + EntityGraph N+1 방지 ✓ |
| 13 | 05 | wish Phase A | 도메인 | [x] | WishService 추출 + 컨트롤러 위임 (인증 인라인은 Phase B) ✓ |
| 14 | 05 | wish Phase B | 도메인 | [x] | 인라인 5건 흡수 + @Transactional + DomainException 통합 ✓ |
| 15 | 06 | order Phase A+B | 도메인 | [x] | OrderService 승격 + OrderFacade 폐기 + OrderCompletedEvent 이벤트화 ✓ |

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
- 2026-05-16: PR #4 (01b-auth Phase A) 완료. `KakaoAuthService` 분리, `MemberService.findOrCreateByKakao` 추가. `./gradlew test` 12/0/0.
- 2026-05-16: PR #5 (01b-auth Phase B) 완료. `DomainException` 계층 + `KakaoLoginException`(422) + `@Transactional` + `extractMemberOrThrow`. `GlobalExceptionHandler` 단일 advice. `./gradlew test` 16/0/0.
- 2026-05-16: PR #6 (01.5-tx-boundary 횡단) 완료. 임시 `OrderFacade @Transactional` 도입으로 주문 6단계 원자화. `OrderFacadeIntegrationTest` 3건 (성공/롤백/404). `@Deprecated(forRemoval, since="01.5")` 박제. `./gradlew test` 19/0/0.
- 2026-05-16: PR #7 (02-product Phase A) 완료. `ProductService` + `CategoryService` 추출, 3 컨트롤러 위임. `./gradlew test` 24/0/0.
- 2026-05-17: PR #8 (02-product Phase B + category Phase B 흡수) 완료. `@Transactional` 부착, `Product` 엔티티 이름 자가검증 + `ProductNameInvalidException`(400), `CategoryInUseException`(409) + `existsByCategoryId`, 글로벌 advice 통합, `AdminProductController` `CategoryService` 위임 정리. 회귀 보호 2건 추가 (`ProductControllerValidationTest`, `CategoryServiceTest.deleteRejectsCategoryReferencedByProduct`).
- 2026-05-17: PR #9 (03-category Phase A) 박제. 실 작업은 PR #7/PR #8 에 모두 흡수됨 (CategoryService 추출 + Phase B 참조 검사). 별도 코드 변경 없음.
- 2026-05-17: PR #10 (04-option Phase A) 완료. `OptionService` 추출 (`findByProductId` / `create` / `delete`), `OptionController` 가 Repository 직접 의존 없이 위임. `OptionServiceTest` 3건 추가 (create 성공 / unknown product null / last option 삭제 거부).
- 2026-05-17: PR #11 (04-option Phase B) 완료. `Option` 엔티티 자가검증 + `OptionNameInvalidException`(400), `LastOptionDeletionException`(422), 중복 → `DuplicateException`(409), `@Transactional` 부착, `delete` 가드 순서 재정렬 (잘못된 optionId → 404 우선), 글로벌 advice 통합 + `OptionNameValidator` 삭제. 회귀 보호 3건 추가 (`OptionServiceTest.createRejectsDuplicateName`, `OptionControllerValidationTest` illegal-name 400 + last-option 422).
- 2026-05-17: PR #12 (04.5-fk-unification 횡단) 완료. Wish/Order 의 primitive `memberId` → `Member @ManyToOne(LAZY)` 매핑 통일 (ADR-003), `@JoinColumn(name="member_id")` 로 V1 스키마와 정합. `findByMember_Id` Spring Data 중첩 표기 + `@EntityGraph` (Wish→product, Order→option/option.product) N+1 방지. 호출처 (WishController, OrderController, OrderFacade) 일괄 갱신. 별도 마이그레이션 없음. 32/0/0 회귀 통과.
- 2026-05-17: PR #13 (05-wish Phase A) 완료. `WishService` 추출 — `list` / `add`(`AddOutcome`) / `remove`(`RemoveOutcome` enum) / `removeByMemberAndProduct`. `WishController` 가 Repository 의존 없이 위임, 응답 코드 401/404/200/201/204/403 보존. 인증 인라인 + product 404 + 소유권 검사는 Phase B 통합 대상. `WishServiceTest` 6건 추가. 38/0/0 그린.
- 2026-05-17: PR #14 (05-wish Phase B) 완료. 인라인 5건 흡수 — `@Transactional`, `extractMemberOrThrow`(401), `NotFoundException`(404), `AuthorizationException`(403), `RemoveOutcome` enum 폐기 (예외 단일 패턴). `WishRepository.findByMember_IdAndProduct_Id` 에 `@EntityGraph(product)` 추가 (PR #12 architect lazy 권고 흡수). 회귀 보호: `WishServiceTest` 갱신 + NotFound 신규, `WishControllerValidationTest` 신규 (401). 40/0/0 그린.
- 2026-05-17: **PR #15 (06-order Phase A+B 합본) 완료 — 15/15 시퀀스 종결**. `OrderService.placeOrder` 로 `OrderFacade.createOrder` 승격 + facade 폐기 (`grep OrderFacade` 0건). 위시 정리 동작 추가 (`wishService.removeByMemberAndProduct`). 카카오 알림 `OrderCompletedEvent` + `KakaoNotificationListener` `@TransactionalEventListener(AFTER_COMMIT)` 이벤트화 (ADR-006a 정합). `KakaoMessageClient.sendToMe(OrderCompletedEvent)` 시그니처 단일화. `OrderServiceIntegrationTest` 4건 (정상 / 롤백 / option not found / wish cleanup). 41/0/0 그린.
- 2026-05-17: PR #15 follow-up. `OrderService` 가 `OptionRepository` / `MemberRepository` 직접 의존을 제거하고 `OptionService.findByIdOrThrow` + `MemberService.findById` 위임 (06-order §A.2 박제 이행). `OrderOptionNotFoundException` 폐기 → `NotFoundException` 일반화. JPA dirty checking 활용으로 중복 save 제거. 41/0/0 유지.
- 2026-05-17: **PR #16 (후속 사이클 — Outbox 패턴, ADR-006b 활성화)** 완료. 카카오 알림 인메모리 listener 폐기 + `outbox_event` 테이블 (Flyway V3) + `OutboxEventProcessor` (재시도 + MAX_ATTEMPTS=5 후 DEAD) + `OutboxPoller` (`@Scheduled(fixedDelay=5초)`). `OrderService.placeOrder` 가 주문 트랜잭션 안에서 outbox 행 INSERT — 모든 비즈니스 로직 성공 + 외부 API 실패 시나리오에서 알림 유실 방지. `OrderServiceIntegrationTest.placeOrderWritesPendingOutboxRow` + `OutboxEventProcessorTest` 4건 (no-token / 성공 / 1회 실패 / MAX_ATTEMPTS 도달 시 DEAD).
