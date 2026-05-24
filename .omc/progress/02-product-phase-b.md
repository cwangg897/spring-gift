# PR #8 (02-product Phase B + category Phase B 흡수) 진행 추적

> 다음 세션 시작점. `/ralph PR #8 진행해줘`로 진입.

## 목표 (refactor-progress/02-product.md §Phase B)
- **B.1**: `ProductService` mutating `@Transactional`, 조회 `readOnly=true`. `CategoryService` 도 동일 패턴
- **B.2**: **Product 이름 검증 위치 이동 (ADR-004)** — `ProductNameValidator` 형식 규칙을 `Product` 엔티티 생성자/`update` 안으로, `allowKakao` 분기는 `ProductService.create/update` 인자로
- **B.3**: **카테고리 삭제 시 product 참조 검사 (category Phase B 흡수)** — `CategoryService.delete()` 가 `ProductRepository.existsByCategoryId(Long)` 호출, 참조 있으면 `DomainException`(422) (`CategoryInUseException` 또는 그대로 `DomainException`)
- **B.4**: 자체 `@ExceptionHandler` 제거 — `ProductController.handleIllegalArgument` 삭제, 글로벌 `@RestControllerAdvice` 가 처리

## 단계별 진행 (다음 세션 첫 미체크부터)

- [ ] 1. `ProductService` / `CategoryService` 에 `@Transactional` 부여 (클래스 레벨 `readOnly=true` + mutating 메서드 오버라이드)
- [ ] 2. `Product` 엔티티 생성자에 이름 검증 (`name not blank, length, 허용 문자`) 박제
  - admin 의 form-level "에러 리스트" UX 보존 위해 `ProductService.validateNameOnly` 유지 (정적 호출 형태)
  - `allowKakao` 분기는 `ProductService.create/createForAdmin` 시그니처에 명시
- [ ] 3. `ProductRepository.existsByCategoryId(Long)` 추가 (Spring Data 자동 구현)
- [ ] 4. `CategoryService.delete()` 가 참조 검사 + 참조 시 `DomainException`(422) — `CategoryInUseException extends DomainException`(409 또는 422 결정)
  - 권장: `CategoryInUseException extends DuplicateException`(409 Conflict) 또는 신규 `extends DomainException`(422) — 어느 쪽이든 ADR-007 계열
- [ ] 5. `ProductController.handleIllegalArgument` 제거 (B.4) — 글로벌 advice 가 처리
  - 단, 현재 `ProductService` 가 `IllegalArgumentException` 을 throw — `DomainException` 계열로 마이그레이션 필요. `ProductNameInvalidException extends DomainException`(400) 신설 또는 기존 `IllegalArgumentException` 을 글로벌 advice 에 추가 핸들러.
- [ ] 6. R1 권고: `AdminProductController.categoryRepository.findAll()` → `CategoryService.findAll()` (의존 정리)
- [ ] 7. 테스트: 통합 테스트 (이름 검증 위치 이동 / 카테고리 참조 시 삭제 거부 / Phase B 회귀)
- [ ] 8. `./gradlew test` 그린
- [ ] 9. doc 갱신 + Architect 검증 + 커밋 분리 + cancel

## Phase B 원칙 (작동 변경, 증거 필요)
- 응답코드 변경 명시: 카테고리 참조 시 삭제 → 새 응답 (422 또는 409). 기존 200/204 와 충돌 시 의도된 변경
- `ProductNameValidator` 가 폐기되어도 admin form-level "에러 리스트" UX 보존 (R2)
- `@Transactional` 부착으로 부분 실패 방지

## 환경 메모
- Java 21 launcher 필수
- Docker Desktop 가동 필요
- 현재 25/0/0 (24 + 컨트롤러 어드바이스 회귀 보호 1건 가정)

## Architect 의 PR #7 잔여 권고 (본 PR에 흡수)
- R1: AdminProductController 의 CategoryRepository 의존 정리
- R2: validateNameOnly Phase B 처분 (admin UX 보존)
- R3: REST 경로 throw 통일 (B.4 후 자연스럽게)
- R4: `CategoryService.findById` 활용 (B.3)
