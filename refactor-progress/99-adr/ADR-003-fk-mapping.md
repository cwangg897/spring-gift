# ADR-003 · JPA `@ManyToOne(LAZY)` 통일 + N+1 정책 (V1 스키마 정정 포함)

## Status
Accepted (2026-05-16)

## Context — V1 스키마 사실관계 정정 (V3 박제)
- 현재 JPA 매핑은 **혼합**:
  - `Wish.memberId: Long` (primitive) + `Wish.product: @ManyToOne Product`
  - `Order.memberId: Long` (primitive) + `Order.option: @ManyToOne Option`
- **DB 스키마는 이미 정합**:
  - `db/migration/V1__Initialize_project_tables.sql:31-32`: `wish.member_id bigint not null, foreign key (member_id) references member (id)`
  - `V1__Initialize_project_tables.sql:51`: `orders.member_id bigint not null, foreign key (member_id) references member (id)`
  - 따라서 **Flyway V3 마이그레이션 불필요**. JPA 엔티티 매핑만 통일.
- 현재 사용 패턴: `findByMemberId(Long)`, `findByMemberIdAndProductId(Long, Long)` 가 전부.

## Decision

### 1) 매핑 통일
- 모든 도메인 간 참조를 **`@ManyToOne(fetch = LAZY)`** 로 통일.
- `Wish.memberId: Long` → `Wish.member: Member` (`@ManyToOne(fetch=LAZY) @JoinColumn(name="member_id")`)
- `Order.memberId: Long` → `Order.member: Member` (동일 패턴)
- `@JoinColumn(name="member_id")` 명시로 V1 스키마 컬럼명과 정합.

### 2) N+1 정책
- **쓰기 경로**: `@ManyToOne(LAZY)` 그대로. lazy 프록시는 트랜잭션 안에서만 사용.
- **읽기 경로** (목록 응답): 다음 중 하나로 N+1 방지:
  - (i) **JPQL projection**: `select new gift.wish.WishResponse(w.id, w.product.id, ...) from Wish w where w.member.id = :memberId`
  - (ii) **`@EntityGraph(attributePaths = {"product", "member"})`** on repository method
- 읽기 경로별 적용 위치 박제 (04.5-fk-unification §12.4):
  - `WishController.getWishes` (`WishController.java:38-49`)
  - `OrderController.getOrders` (`OrderController.java:47-59`)
  - `OptionController.getOptions` (`OptionController.java:36-45`) — 이미 `findByProductId(Long)` 라 N+1 영향 적으나 확인.

### 3) 검증
- Hibernate Statistics 활용 통합 테스트로 쿼리 카운트 측정.
- 페이지 크기 N 일 때 SQL 실행 횟수 **≤ 2** (count + select).

## Drivers
- 도메인 표현력 (member 객체 직접 접근 vs ID 매번 조회)
- 일관성 (혼합 매핑 → 단일 패턴)
- 향후 확장성 (member 정보 자연스럽게 접근)
- 권한 검사 자연스러움 (`wish.getMember().equals(currentMember)` vs `wish.getMemberId().equals(currentMember.getId())`)

## Alternatives considered

### Alt-1: primitive `Long` 통일
- **장점**: 변경 면적 더 적음 (현재 `findByMemberId` 만 사용하면 호출처 변경 거의 없음). Vaughn Vernon의 *Effective Aggregate Design* (aggregate 간 참조는 ID로) 패턴과 정합.
- **단점**:
  - 권한 검사 등에서 ID 비교 보일러플레이트 증가
  - 객체 그래프 표현 어색 (Wish → Product 는 `@ManyToOne`인데 Wish → Member 는 ID)
  - 향후 member 정보가 wish/order 응답에 포함될 때 추가 join 필요
- **탈락 이유**: 도메인 표현력·일관성 우선. 변경 면적은 04.5 단일 PR 로 흡수 가능.

### Alt-2: 혼합 유지
- 일관성 부재로 학습 가치 낮음. 탈락.

## Why chosen
- 일관성 + 도메인 표현력 + 향후 확장성. 변경 면적은 04.5 단일 PR 로 격리.
- N+1 정책 박제로 단점 보완.

## Consequences
- 04.5-fk-unification PR이 wish/order 양쪽 생성자 시그니처 변경 (`new Wish(memberId, product)` → `new Wish(member, product)`).
- Repository 메서드 시그니처 변경 또는 JPQL 명시 (`where w.member.id = ?1`).
- 읽기 경로마다 projection/EntityGraph 박제 필수.
- 트랜잭션 외부에서 lazy 접근 시 `LazyInitializationException` 위험 — 응답 변환을 트랜잭션 안에서 수행하는 패턴 유지.

## Follow-ups
- 페이지네이션 응답 표준화 (별도 후속 작업).
- DDD aggregate 경계가 엄격해지면 Alt-1로 재검토 가능 (현 사이클 범위 외).

## 적용 PR
- 04.5-fk-unification (단일 PR, JPA 매핑만)
- 05-wish Phase A/B (호출처 갱신)
- 06-order Phase A (호출처 갱신)
