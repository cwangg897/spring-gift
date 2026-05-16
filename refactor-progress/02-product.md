# 02-product · 상품 도메인 (+ category Phase B 흡수)

> **PR #7 (Phase A) + PR #8 (Phase B, category B 흡수)**.
> **흡수 결정**: 03-category Phase B 를 본 도메인 Phase B 에 합본. category 는 product 종속 CRUD.

---

## 1. 현재 상태 진단

- 패키지: `gift.product`, `gift.category`
- 대상 파일:
  - `ProductController.java:33-101` — CRUD 비즈니스 로직 인라인 (`categoryRepository.findById`, `productRepository.save`)
  - `ProductController.java:90-95` — `validateName` 정적 헬퍼 호출
  - `ProductController.java:97-100` — 자체 `@ExceptionHandler`
  - `ProductNameValidator.java:1-41` — 정적 유틸 (형식 + `allowKakao` 비즈니스 분기 혼재)
  - `AdminProductController.java:40-92` — Thymeleaf, repository 직접 호출 + `allowKakao=true` 분기
  - `CategoryController.java:34-60` — CRUD repository 직접 호출

---

## 2. 목표 산출물 + Phase A/B 체크리스트

### Phase A (PR #7) — 구조 변경

- [ ] A.1 `gift.product.ProductService` 신설 — `create/update/delete/findById/findAll(Pageable)`, 컨트롤러 위임
- [ ] A.2 `gift.category.CategoryService` 신설 (Phase A 만) — `CategoryController` 위임

### Phase B (PR #8, product + category 합본) — 작동 변경

- [ ] B.1 `ProductService` mutating `@Transactional`, 조회 `readOnly=true`
- [ ] B.2 **Product 이름 검증 위치 이동 (ADR-004)** — `ProductNameValidator` 형식 규칙을 `Product` 생성자/`update` 안으로, `allowKakao` 분기는 `ProductService.create/update` 인자로
- [ ] B.3 **카테고리 삭제 시 product 참조 검사 (category Phase B 흡수)** — `CategoryService.delete()` 가 `ProductRepository.existsByCategoryId(Long)` 호출, 참조 시 `DomainException`(422)
- [ ] B.4 자체 `@ExceptionHandler` 제거 (ADR-007 글로벌 advice 가 처리)

---

## 3. 검증 명령

```
./gradlew test
```

---

## 4. 변경 로그

- _(작업 진행 시 기록)_
