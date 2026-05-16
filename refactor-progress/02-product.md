# 02-product · 상품 도메인 (+ category Phase B 흡수)

> **PR #7 (Phase A) + PR #8 (Phase B, category B 흡수) / 15**.
> **흡수 결정 (V3-4)**: 03-category Phase B를 본 도메인 Phase B에 흡수. category는 product 종속적이고 B 작업량이 적음 → PR diff ≤ 400줄 예상.

---

## [필수 1] 0단계 그린 캡처

- 직전 PR: `01.5-tx-boundary` (PR #6)
- 그린 캡처: 6.1~6.4 + `OrderFacadeIntegrationTest` 그린, `OrderFacade @Deprecated(forRemoval)` 박제 — 커밋 SHA 첨부

---

## [필수 2] 현재 상태 진단

- 패키지: `gift.product`, `gift.category`
- 대상 파일:
  - `src/main/java/gift/product/ProductController.java:33-101` — 모든 CRUD 비즈니스 로직 인라인 (`categoryRepository.findById`, `productRepository.save`)
  - `src/main/java/gift/product/ProductController.java:90-95` — `validateName` 정적 헬퍼 호출
  - `src/main/java/gift/product/ProductController.java:97-100` — 컨트롤러별 `@ExceptionHandler` (ADR-007 통합 대상)
  - `src/main/java/gift/product/ProductNameValidator.java:1-41` — 정적 유틸 (형식 검증 + `allowKakao` 비즈니스 분기 혼재)
  - `src/main/java/gift/product/AdminProductController.java:40-92` — Thymeleaf, repository 직접 호출 + `allowKakao=true` 분기 사용
  - `src/main/java/gift/category/CategoryController.java:34-60` — CRUD repository 직접 호출
- 외부 의존: 01a/01b (auth 통합 예외 처리)

---

## [필수 3] 목표 산출물 + Phase A/B 체크리스트

### Phase A (PR #7) — 구조 변경 (작동 동일)

- [ ] **A.1** `gift.product.ProductService` 신설 — (a) `create/update/delete/findById/findAll(Pageable)` 메서드 (b) `ProductController` + `AdminProductController` 가 service 위임만, repository 직접 호출 grep 결과 product 패키지 controller에서 0건 (c) `ProductServiceTest` + `ProductControllerTest` (WebMvcTest) 그린
- [ ] **A.2** `gift.category.CategoryService` 신설 (Phase A만, B는 본 도메인 Phase B 흡수) — (a) `create/update/delete/findAll/findById` (b) `CategoryController` 위임 (c) `./gradlew test --tests "gift.category.*"` 그린

### Phase B (PR #8, product + category 합본) — 작동 변경

- [ ] **B.1** `ProductService` 메서드에 `@Transactional` 부여 — (a) mutating 메서드 `@Transactional`, 조회 `readOnly=true` (b) ADR-002 정합 (c) 부분 실패 회귀 보호 테스트
- [ ] **B.2** **Product 이름 검증 위치 이동 (ADR-004 매트릭스)** — (a) `ProductNameValidator` 의 형식 규칙(길이/허용문자/`@NotBlank`)을 `Product` 엔티티 생성자(`Product.java:36-41`)와 `update` 메서드 안으로 이동 (b) `allowKakao` 분기(`ProductNameValidator.java:35-37`)는 `ProductService.create/update` 인자로 전달 (관리자 컨텍스트 표시), controller 의 `validateName` 호출 grep 결과 0건 (c) 경계값 단위 테스트 (`ProductDomainTest`)
- [ ] **B.3** **카테고리 삭제 시 product 참조 검사 추가 (category Phase B 흡수)** — (a) `CategoryService.delete(Long)` 가 `ProductRepository` 의 `existsByCategoryId(Long)` 호출 (b) repository 메서드 추가 (c) 통합 테스트: product 가 참조 중인 카테고리 삭제 시 `DomainException`(422)
- [ ] **B.4** `@ExceptionHandler` 제거 (ADR-007 통합) — (a) `ProductController.java:97-100` 와 `OptionController` 의 자체 핸들러 제거, 글로벌 `@RestControllerAdvice` 가 처리 (b) 자체 핸들러 grep 결과 0건 (c) 422 응답 통합 테스트

---

## [필수 4] 검증 명령 + README 게이트

```
./gradlew test --tests "gift.product.*"
./gradlew test --tests "gift.category.*"
./gradlew test
```

게이트:
- [ ] PR #7, #8 머지 시 README 진행률 갱신 (02, 03 흡수 표시)

---

## [선택 1] 작동 변경 vs 구조 변경

| 변경 ID | 종류 | 관련 ADR |
|---|---|---|
| A.1, A.2 | 구조 | — |
| B.1 | 작동 | ADR-002 |
| B.2 | 작동 (도메인 책임 회수) | ADR-004 |
| B.3 | 작동 (누락 동작 구현) | — |
| B.4 | 작동 (응답 코드 통일) | ADR-007 |

## [선택 3] 관련 ADR

- [ADR-002](./99-adr/ADR-002-tx-boundary.md)
- [ADR-004](./99-adr/ADR-004-validation-matrix.md) — anti-pattern ban 적용
- [ADR-007](./99-adr/ADR-007-concurrency.md)

## [선택 5] 변경 로그

- _(작업 진행 시 기록)_
