# 03-category · 카테고리 도메인 (Phase A 만)

> **PR #9**. Phase B 는 [02-product Phase B](./02-product.md) 에 흡수 (V3-4 결정).

---

## 1. 현재 상태 진단

- 패키지: `gift.category`
- 대상 파일:
  - `CategoryController.java:34-60` — CRUD repository 직접 호출
  - `Category.java:28-33` — `update` 메서드 존재 (유지)
- 외부 의존: `gift.product.Product` (category 참조)

---

## 2. 목표 산출물 + Phase A 체크리스트

### Phase A — 구조 변경

- [ ] A.1 `gift.category.CategoryService` 신설 — `create/update/delete/findAll/findById`, `CategoryController` 위임

### Phase B → 02-product Phase B 에 흡수

- 카테고리 삭제 시 product 참조 검사는 [02-product.md §B.3](./02-product.md) 참조.

---

## 3. 검증 명령

```
./gradlew test
```

---

## 4. 변경 로그

- _(작업 진행 시 기록)_
