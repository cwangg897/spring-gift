# ADR-002 · 트랜잭션 경계 = 서비스 계층

## Status
Accepted (2026-05-16)

## Context
- 현재 `OrderController.createOrder` (`OrderController.java:69-102`)가 6단계 무트랜잭션 → 부분 실패 시 재고/포인트/주문/위시 정합성 깨짐.
- 다른 컨트롤러도 `@Transactional` 0개.
- prd 라인 9: "트랜잭션 경계 세우기" — 작동 변경의 첫째 항목.

## Decision
**모든 비즈니스 트랜잭션 경계는 서비스 계층(또는 임시 facade)에 둔다**.
- 컨트롤러: 트랜잭션 시작점이 아니다. 인증/응답 직렬화만.
- 리포지토리: 트랜잭션 시작점이 아니다. JPA 자동 트랜잭션이 외부 service 트랜잭션에 join.
- 서비스 메서드: mutating 메서드는 `@Transactional`, 조회는 `@Transactional(readOnly=true)`.
- 임시 facade (01.5의 `OrderFacade`)는 한시적이며 06-order Phase A에서 정식 service로 승격 후 폐기.

## Drivers
- 부분 실패 방지 (운영 위험 1순위)
- 도메인 책임 명확화
- prd 라인 9 우선순위

## Alternatives considered
- **컨트롤러 `@Transactional`**: 웹 계층 오염, 테스트 어려움. 탈락.
- **수동 트랜잭션 관리 (PlatformTransactionManager)**: 복잡도↑, 학습 가치 낮음. 탈락.
- **Repository `@Transactional`**: 트랜잭션 범위가 단일 쿼리로 한정 → 6단계 흐름 보호 불가. 탈락.

## Why chosen
- 서비스 = 비즈니스 흐름 조립자 = 트랜잭션 경계의 자연스러운 위치.
- Spring 표준 관용구.
- 01.5 임시 facade 패턴이 점진적 도입 가능하게 함.

## Consequences
- 모든 도메인 service의 mutating 메서드에 `@Transactional` 박제 필요.
- 01.5의 `OrderFacade` → 06-order의 `OrderService` 승격 경로 고정.
- `AuthenticationResolver` 등 인프라성 컴포넌트는 트랜잭션 없이 동작 가능 (DB 미접근 또는 단일 조회만).

## Follow-ups
- 트랜잭션 격리 수준은 기본 (`DEFAULT`) 유지, 동시성 이슈는 ADR-007에서 다룸.
- `@TransactionalEventListener` (AFTER_COMMIT) 는 ADR-006a 참조.

## 적용 PR
- 01a-member Phase B.1
- 01b-auth Phase B.1
- 01.5-tx-boundary (임시 facade, 핵심 위험 즉시 해결)
- 02-product Phase B.1, 04-option Phase B.4, 05-wish Phase B.5, 06-order Phase A.1
