# 03-category · 카테고리 도메인 (Phase A만)

> **PR #9 / 15**. Phase B는 [02-product Phase B](./02-product.md)에 **흡수** (V3-4 결정).
> **이유**: category는 product 종속 CRUD, B 작업량(삭제 시 참조 검사)이 적어 단일 PR로 흡수가 효율적.

---

## [필수 1] 0단계 그린 캡처

- 직전 PR: `02-product Phase B` (PR #8, category Phase B 흡수 포함)
- 그린 캡처: 02 통합 테스트 + 전체 `./gradlew test` 메인에서 그린 — 커밋 SHA 첨부

---

## [필수 2] 현재 상태 진단

- 패키지: `gift.category`
- 대상 파일:
  - `src/main/java/gift/category/CategoryController.java:34-60` — CRUD repository 직접 호출
  - `src/main/java/gift/category/Category.java:28-33` — `update` 메서드 존재 (도메인 메서드, 유지)
  - `src/main/java/gift/category/CategoryRepository.java:1-7` — 기본 JpaRepository
- 외부 의존: `gift.product.Product` (category 참조)

---

## [필수 3] 목표 산출물 + Phase A 체크리스트

### Phase A — 구조 변경 (작동 동일)

- [ ] **A.1** `gift.category.CategoryService` 신설 — (a) `create(CategoryRequest)` / `update(Long, CategoryRequest)` / `delete(Long)` / `findAll()` / `findById(Long)` (b) `CategoryController` 위임만, `categoryRepository` 직접 호출 grep 결과 controller 에서 0건 (c) `CategoryServiceTest` + `CategoryControllerTest` (WebMvcTest) 그린

### Phase B → 02-product Phase B에 흡수

다음 항목은 본 문서에서 추적하지 않고 [02-product.md §B.3](./02-product.md)에서 추적:
- 카테고리 삭제 시 product 참조 검사 추가

---

## [필수 4] 검증 명령 + README 게이트

```
./gradlew test --tests "gift.category.*"
./gradlew test
```

게이트:
- [ ] PR #9 머지 시 README 진행률 갱신

---

## [선택 1] 작동 변경 vs 구조 변경

| 변경 ID | 종류 | 관련 ADR |
|---|---|---|
| A.1 | 구조 | — |
| (B 흡수) | 02-product B.3 참조 | — |

## [선택 3] 관련 ADR

- [ADR-002](./99-adr/ADR-002-tx-boundary.md) — service 계층 표준

## [선택 5] 변경 로그

- _(작업 진행 시 기록)_
