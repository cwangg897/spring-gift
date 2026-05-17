# 04-option · 옵션 도메인

> **PR #10 (Phase A) + PR #11 (Phase B)**.
> **핵심 사실**: `Option.subtractQuantity` / `Member.deductPoint` 는 이미 엔티티에 존재. 추가 이전 작업 없음. Phase B 는 (1) 이름 검증 위치 이동 (2) "마지막 옵션 삭제 금지" 이동.

---

## 1. 현재 상태 진단

- 패키지: `gift.option`
- 대상 파일:
  - `Option.java:33-37` — 생성자에 이름 검증 미내장 (컨트롤러에서 정적 호출 중)
  - `Option.java:39-44` — `subtractQuantity(int)` **이미 엔티티 존재** + 음수 가드 (유지)
  - `OptionController.java:52` — `validateName(request.name())` 호출 (이동 대상)
  - `OptionController.java:93-98` — `validateName` 헬퍼 (이동 대상)
  - `OptionController.java:80-82` — "마지막 옵션 삭제 금지" 인라인 (이동 대상)
  - `OptionController.java:59-61` — 옵션명 중복 검사 인라인
  - `OptionController.java:100-103` — 자체 `@ExceptionHandler`
  - `OptionNameValidator.java:1-39` — 정적 유틸 (폐기 또는 `Option` private 헬퍼화)
  - `Member.java:57-65` — `deductPoint` **이미 엔티티 존재** (유지)
  - `OrderController.java:87,92` — `option.subtractQuantity` / `member.deductPoint` 이미 위임 호출 중 (유지)

---

## 2. 목표 산출물 + Phase A/B 체크리스트

### Phase A (PR #10) — 구조 변경

- [x] A.1 `gift.option.OptionService` 신설 — `findByProductId / create / delete`, 컨트롤러 위임

### Phase B (PR #11) — 작동 변경

- [x] B.1 Option 이름 검증을 `Option` 생성자 `validateNameFormat` 으로 박제, `OptionNameValidator` 폐기 (엔티티 내부 private 헬퍼로 흡수), `OptionNameInvalidException`(400) 도입.
- [x] B.2 "옵션 ≥2 일 때만 삭제 가능" 규칙을 `OptionService.delete()` 로 이동 + `LastOptionDeletionException`(422), 가드 순서 재정렬 (product 존재 → option 존재+소유 확인 → last-option 검사).
- [x] B.3 `OptionService.create/delete` `@Transactional`, 중복 이름은 `DuplicateException`(409) 로 변환.
- [x] B.4 `OptionController` 자체 `@ExceptionHandler` 제거 — 글로벌 `GlobalExceptionHandler` 가 처리.

> **검증/문서화**: `Option.subtractQuantity` (Option.java:39-44), `Member.deductPoint` (Member.java:57-65) 는 이미 엔티티에 있음을 PR description 에 명시. 추가 코드 작업 없음.

> **Race condition**: 마지막 옵션 동시 삭제 시 둘 다 통과 가능 → ADR-007 후속 사이클.

---

## 3. 검증 명령

```
./gradlew test
grep -rn "OptionNameValidator" src/   # B.1 완료 시 0건 또는 Option 내부만
```

---

## 4. 변경 로그

- 2026-05-17: Phase A 완료 — `OptionService` 추출 (findByProductId / create / delete), `OptionController` 위임 전환 (Repository 의존 제거). `OptionServiceTest` 3건 추가 (create 성공 / unknown product null / last option 삭제 거부).
- 2026-05-17: Phase B 완료 — `Option` 엔티티 자가검증 + `OptionNameInvalidException`(400), `LastOptionDeletionException`(422), 중복 → `DuplicateException`(409), `@Transactional` 부착, `delete` 가드 순서 재정렬 (잘못된 optionId → 404 우선), `OptionController` 자체 `@ExceptionHandler` 제거 (글로벌 advice 통합), `OptionNameValidator.java` 삭제. 회귀 보호 3건 추가 (`OptionServiceTest.createRejectsDuplicateName`, `OptionControllerValidationTest`의 illegal-name 400 + last-option 422).
