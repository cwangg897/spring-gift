# ADR-007 · 동시성 제약 사항 + 예외 계층 통합

## Status
Accepted (예외 계층 통합은 본 사이클 적용 / 동시성 보호는 범위 외)

## Context

### 동시성 위험 (코드 사실)
- `Option.subtractQuantity(int)` (`Option.java:39-44`): 인메모리 가드만 (`if (amount > this.quantity) throw ...`). 두 트랜잭션이 같은 `quantity=5` 읽고 각자 차감하면 over-sell.
- `OptionService.delete` (04 Phase B.3 신설 예정): 동시 삭제 시 두 트랜잭션 모두 `size()=2` 읽어 둘 다 통과 가능 → `size()=0` 결과.
- `WishService.add` (05 Phase B.3 신설 예정): 중복 검사 → 추가 사이 race 가능.

### 예외 처리 분산
- 각 컨트롤러가 자체 `@ExceptionHandler` 보유:
  - `MemberController.java:57-60`
  - `ProductController.java:97-100`
  - `OptionController.java:100-103`
- `WishController`, `OrderController` 는 if-null-401/404 인라인 처리.
- 응답 코드가 도메인별로 일관성 없음.

## Decision

### Part 1 — 예외 계층 통합 (본 사이클 적용)

#### 1.1 도메인 예외 계층
```
RuntimeException
 └─ DomainException (abstract, → 422 Unprocessable Entity)
     ├─ NotFoundException (→ 404)
     ├─ AuthenticationException (→ 401)
     ├─ AuthorizationException (→ 403)
     ├─ DuplicateException (→ 409)
     └─ (그 외 도메인 위반은 422)
```

#### 1.2 글로벌 핸들러 (`@RestControllerAdvice`)
- `gift.support.GlobalExceptionHandler`:
  - `MethodArgumentNotValidException` → 400 (Bean Validation)
  - `AuthenticationException` → 401
  - `AuthorizationException` → 403
  - `NotFoundException` → 404
  - `DuplicateException` → 409
  - `DomainException` → 422
  - 그 외 RuntimeException → 500 (로깅)

#### 1.3 컨트롤러 정리
- 각 컨트롤러의 자체 `@ExceptionHandler` 모두 제거.
- if-null-XXX 패턴 모두 service의 throw 또는 글로벌 advice 처리로 이동.

### Part 2 — 동시성 보호 (본 사이클 범위 외)

#### 2.1 인지된 race condition (해결 미룸)
| 위치 | 위험 | 본 사이클 처리 |
|---|---|---|
| `Option.subtractQuantity` 동시 호출 | over-sell | **범위 외**, 단일 사용자 시나리오 가정 |
| 마지막 옵션 동시 삭제 (04 B.3) | size=0 결과 | **범위 외**, ADR 메모 |
| 위시 중복 추가 race (05 B.3) | 중복 행 | DB unique constraint 후속 검토 |

#### 2.2 활성화 시 옵션 (후속 사이클)
- **낙관락**: `@Version` 컬럼 (변경 면적 작음, retry 책임 호출자)
- **비관락**: `select ... for update` + `@Lock(LockModeType.PESSIMISTIC_WRITE)` (간단, 처리량 영향)
- **DB unique constraint**: `wish(member_id, product_id) UNIQUE` 같은 제약 (DB 레벨 보장)

#### 2.3 활성화 조건
- 운영 트래픽 증가로 동시 주문 발생 빈도가 의미 있어졌을 때.
- 학습 사이클에서는 의도적으로 범위 외.

## Drivers
- **예외 계층**: 응답 코드 일관성, 컨트롤러 try-catch 제거, 도메인 의도 표현.
- **동시성**: 학습 과제의 단일 사용자 시나리오 가정. 인프라 복잡도 보류.

## Alternatives considered

### 예외 처리
- **컨트롤러별 try-catch 유지**: 분산 + 일관성 부재. 탈락.
- **RuntimeException 일괄 처리**: 401/403/404/422 구분 불가. 탈락.

### 동시성
- **본 사이클에 동시성 보호 추가**: 과도한 범위 확장. 탈락.

## Why chosen
- 예외 계층은 응답 일관성과 컨트롤러 간결화에 필수 → 본 사이클 적용.
- 동시성은 운영 트래픽 컨텍스트가 필요한 결정 → 후속 보류 박제.

## Consequences
- 도메인 예외 클래스 5~6개 추가 (`gift.support.exception` 패키지 후보).
- 모든 컨트롤러의 `@ExceptionHandler` 제거.
- 응답 메시지 포맷 통일 가능 (별도 ProblemDetail 도입 후속).

## Follow-ups
- ProblemDetail (RFC 7807) 응답 표준화 (후속).
- 동시성 보호 활성화 (운영 트래픽 트리거).
- DB 제약 추가 (wish unique, option subtract 트리거 등).

## 적용 PR
- 01b-auth Phase B.2, B.3 (AuthenticationException, KakaoLoginException)
- 02-product Phase B.4 (자체 핸들러 제거)
- 04-option Phase B.3 (LastOptionDeletionException), B.4 (DuplicateException)
- 05-wish Phase B.1, B.2, B.4 (인라인 → 예외 + 글로벌 advice)
- (전제) `gift.support.GlobalExceptionHandler` 신설 — 어느 PR 에서 만들지 결정 필요: 권장은 **01b-auth Phase B.3** 에서 함께 도입 (그 다음 도메인부터 활용).
