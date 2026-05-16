# 04-option · 옵션 도메인 (V3-1 Phase B 전면 재정의)

> **PR #10 (Phase A) + PR #11 (Phase B) / 15**.
> **V3-1 핵심 변경**: 코드 사실 확인 결과 `Option.subtractQuantity`/`Member.deductPoint` 이미 엔티티 존재 → 무의미한 "엔티티 이전" 폐기. Phase B는 (1) 검증/문서화 (2) Option 이름 검증 이동 (3) "마지막 옵션 삭제 금지" 이동.

---

## [필수 1] 0단계 그린 캡처

- 직전 PR: `03-category Phase A` (PR #9)
- 그린 캡처: 03 + 02 + 모든 상류 도메인 통합 테스트 그린 — 커밋 SHA 첨부

---

## [필수 2] 현재 상태 진단

- 패키지: `gift.option`
- 대상 파일:
  - `src/main/java/gift/option/Option.java:33-37` — 생성자에 **이름 검증 미내장** (현재 컨트롤러에서 정적 호출)
  - `src/main/java/gift/option/Option.java:39-44` — `subtractQuantity(int)` **이미 엔티티 존재 + 음수 차감 가드**. **유지**, 재이전 작업 없음
  - `src/main/java/gift/option/OptionController.java:52` — `validateName(request.name())` 호출 (이동 대상)
  - `src/main/java/gift/option/OptionController.java:93-98` — `validateName` 헬퍼 (이동 대상)
  - `src/main/java/gift/option/OptionController.java:80-82` — "마지막 옵션 삭제 금지" 인라인 (`if (options.size() <= 1) throw ...`) (이동 대상)
  - `src/main/java/gift/option/OptionController.java:59-61` — 옵션명 중복 검사 인라인 (`existsByProductIdAndName`) — service로 이동 후보
  - `src/main/java/gift/option/OptionController.java:100-103` — 자체 `@ExceptionHandler` (ADR-007 통합 대상)
  - `src/main/java/gift/option/OptionNameValidator.java:1-39` — 정적 유틸 (폐기 또는 `Option` private 헬퍼화)
  - `src/main/java/gift/member/Member.java:57-65` — `deductPoint` **이미 엔티티 존재**. 검증/문서화만
  - `src/main/java/gift/order/OrderController.java:87` — `option.subtractQuantity(...)` **이미 위임 호출 중**. 추가 작업 없음
  - `src/main/java/gift/order/OrderController.java:92` — `member.deductPoint(price)` **이미 위임 호출 중**. 추가 작업 없음
- 외부 의존: 02-product (Product 참조), 01.5 (OrderFacade가 subtractQuantity 호출)

---

## [필수 3] 목표 산출물 + Phase A/B 체크리스트

### Phase A (PR #10) — 구조 변경 (작동 동일)

- [ ] **A.1** `gift.option.OptionService` 신설 — (a) `findByProductId(Long)` / `create(Long productId, OptionRequest)` / `delete(Long productId, Long optionId)` (b) `OptionController` 위임만, `optionRepository`/`productRepository` 직접 호출 grep 결과 controller 에서 0건 (c) `OptionServiceTest` + `OptionControllerTest` (WebMvcTest) 그린

### Phase B (PR #11) — V3-1 전면 재정의 (작동 변경)

#### B.1 검증/문서화 (V3-1 박제: 엔티티 이미 존재)

- [ ] **B.1** **`Option.subtractQuantity`(Option.java:39-44), `Member.deductPoint`(Member.java:57-65) 엔티티 이미 존재 + OrderController.java:87,92 위임 호출 중** — (a) 추가 코드 작업 없음 (b) 위임 현황을 PR description 에 캡처 (c) 두 엔티티 메서드의 도메인 단위 테스트 ≥4 (경계값: 0, 음수, 정확히 잔량, 잔량 초과) — 없으면 추가, 있으면 명시

#### B.2 Option 이름 검증 위치 이동 (ADR-004 매트릭스 적용)

- [ ] **B.2** Option 이름 검증을 `Option` 엔티티 생성자로 이동 — (a) `Option.java:33-37` 생성자에 검증 (길이 ≤ 50, 허용 문자 집합, 빈문자 금지) 박제. 또는 private static `validateName(String)` 헬퍼로 격리 (b) `OptionController.java:52,93-98` 의 `validateName` 호출/헬퍼 제거. `OptionNameValidator` 폐기 또는 `Option` 내부 private (외부 사용처 grep 0건 시 폐기) (c) `OptionDomainTest` 의 경계 케이스 ≥4 (빈 문자열, 공백만, 길이 50, 길이 51, 허용/비허용 특수문자) 그린

#### B.3 "마지막 옵션 삭제 금지" 규칙 이동

- [ ] **B.3** "옵션 ≥2일 때만 삭제 가능" 규칙을 `OptionService.delete()` 로 이동 — (a) `OptionService.delete(productId, optionId)` 내부에 검사 + 도메인 예외 `LastOptionDeletionException extends DomainException` (422) (b) `OptionController.java:80-82` 의 인라인 검사 제거 (c) `OptionServiceTest`: 옵션 1개 상품의 삭제 시도 → 예외, 옵션 2개 → 정상

#### B.4 옵션 추가 시 `@Transactional` + 중복 방지 service 이동

- [ ] **B.4** 옵션 추가/삭제에 `@Transactional` 부여 + 중복 이름 검사를 service로 — (a) `OptionService.create/delete` 메서드에 `@Transactional`, `existsByProductIdAndName` 호출을 service 내부로 (b) `OptionController.java:59-61` 의 중복 검사 인라인 제거 (c) 동일 이름 옵션 중복 추가 시 422 응답 통합 테스트

> **Race Condition (ADR-007 후속)**: 마지막 옵션 동시 삭제 시 두 트랜잭션 모두 `size()=2`로 읽어 둘 다 통과 가능. 본 사이클 비범위, 후속 작업에서 비관락 또는 DB 제약으로 해결.

---

## [필수 4] 검증 명령 + README 게이트

```
./gradlew test --tests "gift.option.*"
./gradlew test
grep -rn "OptionNameValidator" src/   # B.2 완료 시 0건 또는 Option 내부만
```

게이트:
- [ ] PR #10, #11 머지 시 README 진행률 갱신

---

## [선택 1] 작동 변경 vs 구조 변경

| 변경 ID | 종류 | 관련 ADR |
|---|---|---|
| A.1 | 구조 | — |
| B.1 | 구조 (검증/문서화) | ADR-004 |
| B.2 | 작동 (도메인 책임 회수) | ADR-004 |
| B.3 | 작동 (도메인 책임 회수) | ADR-007 (race 메모) |
| B.4 | 작동 | ADR-002, ADR-007 |

## [선택 3] 관련 ADR

- [ADR-004](./99-adr/ADR-004-validation-matrix.md)
- [ADR-007](./99-adr/ADR-007-concurrency.md) — race condition 후속 메모

## [선택 5] 변경 로그

- _(작업 진행 시 기록)_
