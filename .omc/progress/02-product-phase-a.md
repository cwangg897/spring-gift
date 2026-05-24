# PR #7 (02-product Phase A) 진행 추적

## 목표 (refactor-progress/02-product.md §Phase A)
- **A.1**: `gift.product.ProductService` 신설 — `create/update/delete/findById/findAll(Pageable)`, 컨트롤러 위임
- **A.2**: `gift.category.CategoryService` 신설 (Phase A 만) — `CategoryController` 위임

## 단계별 진행

- [x] 1~7 완료. `./gradlew test` 24/0/0. Architect APPROVE.
- 커밋 분리: `b1dcc94 feat(product)` + `2bd6a11 feat(category)` + `(docs)`
- 잔여 권고 (Architect): R1 (AdminProductController의 CategoryRepository 의존 정리 — Phase B 흡수 권장), R2 (validateNameOnly Phase B 처분 명시)

## Phase A 원칙 (작동 동일)
- 비즈니스 로직 (`validateName`, `findById...orElse(null)`, repository.save) 그대로 이동
- 응답코드 유지 (404 NotFound, 201 Created, 200 OK, 204 NoContent)
- 컨트롤러의 자체 `@ExceptionHandler` 는 유지 (Phase B 에서 글로벌 advice 로 정리)
- `@Transactional` 부착은 Phase B

## 비범위 (Phase B / 02 합본)
- `ProductNameValidator` 폐기/이동 (B.2)
- 카테고리 삭제 시 product 참조 검사 (B.3)
- `@ExceptionHandler` 제거 (B.4)
