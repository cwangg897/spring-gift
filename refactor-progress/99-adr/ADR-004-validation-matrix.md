# ADR-004 · 검증 책임 매트릭스 + anti-pattern ban

## Status
Accepted (2026-05-16)

## Context
- 현재 검증은 3곳에 분산:
  - DTO Bean Validation (`@Valid` + `@NotBlank`, `@Email`, `@Min`) — `MemberRequest`, `ProductRequest` 등
  - 컨트롤러 내부 정적 호출 (`OptionController.java:52` → `OptionNameValidator`, `ProductController.java:91` → `ProductNameValidator`)
  - 엔티티 도메인 메서드 (`Option.subtractQuantity` 음수 가드, `Member.deductPoint` 잔액 가드)
- 정적 Validator(`OptionNameValidator`, `ProductNameValidator`)는 형식 규칙(길이/허용문자)과 비즈니스 규칙(`allowKakao` 분기)을 혼재.
- 도메인 메서드 자가검증과 DTO `@Valid`가 충돌 가능.

## Decision — 검증 책임 매트릭스

| 위치 | 책임 | 예시 |
|---|---|---|
| **DTO** (`@Valid` 어노테이션) | **형식 검증** (구문) | `@NotBlank`, `@Email`, `@Min(1)`, `@Positive`, `@NotNull` |
| **엔티티 생성자/메서드** | **도메인 불변식** (의미) | `Option.subtractQuantity` 음수 가드, `Member.deductPoint` 잔액 가드, 이름 길이/허용문자, 빈 문자열 금지 |
| **서비스** | **교차 엔티티/외부 컨텍스트 규칙** | 이메일 중복(`existsByEmail`), `allowKakao` 관리자 분기, "마지막 옵션 삭제 금지", "카테고리에 product 참조 있을 때 삭제 금지" |

### Anti-pattern Ban (V3-A 박제, 한 줄)

> **DTO `@Valid` 에서 통과한 형식 규칙(`@NotBlank`, `@Email`, `@Min` 등)을 엔티티 생성자/서비스에서 재검증 금지**. 엔티티는 DTO 입력을 신뢰하고 도메인 불변식만 검사한다. **예외**: 외부 입력 경로가 아닌 내부 코드 경로(테스트, 다른 서비스, 시드 데이터)에서 엔티티가 생성될 가능성이 있다면 엔티티 생성자에서 형식 검증을 유지한다 (e.g., Option 이름).

### 적용 매핑 (도메인별)

| 도메인 | 형식 → DTO | 도메인 불변식 → 엔티티 | 교차 규칙 → 서비스 |
|---|---|---|---|
| member | `MemberRequest.@NotBlank @Email` | (현재 없음 — `matchesPassword` 추가 후보) | `existsByEmail` 중복 검사 |
| product | `ProductRequest.@NotBlank @Positive` | `Product` 생성자에 길이/허용문자 검증 (B.2) | `allowKakao` 관리자 분기 (B.2) |
| option | `OptionRequest.@NotBlank @Min(1) @Max(99_999_999)` | `Option` 생성자에 이름 길이/허용문자 (04 B.2), `subtractQuantity` 음수 가드 (이미 존재) | "마지막 옵션 삭제 금지" (04 B.3), 옵션명 중복 검사 (04 B.4) |
| category | `CategoryRequest.@NotBlank` | `Category.update` (이미 존재) | 카테고리 삭제 시 product 참조 검사 (02 B.3, B.B 흡수) |
| wish | `WishRequest.@NotNull` | — | 중복 검사 (05 B.3), 소유권 검사 (05 B.4) |
| order | `OrderRequest.@NotNull @Min(1)` | — | 트랜잭션 + 이벤트 발행 (06) |

## Drivers
- 책임 분산 방지 (정적 Validator의 혼재 제거)
- 테스트 가능성 (도메인 단위 + 서비스 단위 + 통합 테스트로 명확 분리)
- 도메인 풍부화 (엔티티가 자기 불변식 보호)

## Alternatives considered
- **단일 Validator 클래스 유지**: 책임 혼재 지속, 정적 호출이 service 경계와 어긋남. 탈락.
- **모든 검증을 service로**: 엔티티가 invalid 상태로 존재 가능. 도메인 표현력 손해. 탈락.
- **Hibernate Validator 그룹화 (`@Valid(groups=...)`)**: 학습 가치 낮고 복잡도↑. 탈락.

## Why chosen
- 매트릭스로 책임 위치 박제 → 코드 리뷰 시 즉시 판별 가능.
- Anti-pattern ban 으로 이중 검증 차단.

## Consequences
- `OptionNameValidator`, `ProductNameValidator` 정적 클래스는 폐기 또는 엔티티 내부 private 헬퍼로 격리.
- `Option`, `Product` 엔티티 생성자가 비대화 — private 헬퍼로 가독성 유지.
- DTO와 엔티티 검증 메시지가 다를 수 있음 → 사용자에게 노출되는 메시지는 DTO 메시지 우선 (`@RestControllerAdvice` 정책).

## Follow-ups
- 도메인 예외 메시지 다국어화 (별도 작업).
- Bean Validation 커스텀 어노테이션 (`@KakaoProductName` 등) — 비범위.

## 적용 PR
- 01a-member Phase B.2 (matchesPassword)
- 02-product Phase B.2 (ProductNameValidator 분해)
- 04-option Phase B.2 (OptionNameValidator → Option 생성자)
- 04-option Phase B.3, B.4 (서비스 규칙)
- 05-wish Phase B.3, B.4 (서비스 규칙)
