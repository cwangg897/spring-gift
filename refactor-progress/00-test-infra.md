# 00-test-infra · 테스트 인프라 (횡단, 단일 phase)

> **PR #1 / 15**. 모든 후속 도메인 PR의 0단계 기반.
> **유형**: 횡단 (단일 phase)

---

## [필수 1] 0단계 그린 캡처 (최초 PR — 별도 3요소)

> v3 결정 (V3-2): 00-test-infra의 0단계는 trivially-true 회피를 위해 **3요소 모두 그린**일 때만 통과.

3요소 (모두 그린일 때만 ✓):
- (a) `./gradlew build` — 컴파일 + ktlint
- (b) `gift.ApplicationTest.contextLoads()` 신규 추가 후 그린
- (c) Testcontainers MySQL + Flyway V1/V2 적용 그린 (통합 테스트 1건)

→ 직전 PR이 없으므로 위 3요소를 **본 PR이 직접 만들어 그린**으로 만든다. 이후 PR들의 0단계 기준선이 된다.

---

## [필수 2] 현재 상태 진단

- 패키지: `gift` (지원 모듈 `gift.support` 신설 예정)
- 대상 파일:
  - `src/test/java/gift/.gitkeep` — 테스트 0개 (Java 측)
  - `src/test/kotlin/gift/.gitkeep` — 테스트 0개 (Kotlin 측, 본 작업 비범위)
  - `build.gradle.kts:34-41` — `spring-boot-starter-test` 만 있음, Testcontainers 의존성 부재
  - `src/main/resources/application.properties` — 테스트 프로필 분리 부재
  - `src/main/resources/db/migration/V1__Initialize_project_tables.sql`, `V2__Insert_default_data.sql` — Flyway 마이그레이션 2개 (재사용 대상)
- 외부 의존:
  - Docker (Testcontainers 동작 전제, 학습 환경 가정)
  - Spring Boot 3.5.9 → `@ServiceConnection` 사용 가능 (3.1+)

---

## [필수 3] 목표 산출물 + 체크리스트

### 단일 phase (구조 변경 + 인프라 추가, 작동 영향 없음)

- [x] **0.1** `build.gradle.kts`에 Testcontainers 의존성 추가 — `build.gradle.kts:40-42` 에 `spring-boot-testcontainers` + `testcontainers:junit-jupiter` + `testcontainers:mysql` 추가. `./gradlew compileTestJava` 그린 ✓
- [x] **0.2** `src/test/java/gift/ApplicationTest.java` 신규 — `@SpringBootTest` + `@ActiveProfiles("test")` + `contextLoads()`. ApplicationTest H2 위에서 1개 그린 ✓
- [x] **0.3** `src/test/java/gift/support/AbstractIntegrationTest.java` 신규 — `@Testcontainers` + `static final MySQLContainer<?>` + `@ServiceConnection` + `@ActiveProfiles("test")` + `.withReuse(true)`. 통합 테스트 부트업 그린 ✓
- [x] **0.4** `src/test/resources/application-test.properties` 신규 — `spring.flyway.enabled=true`, `spring.jpa.hibernate.ddl-auto=validate`, `spring.jpa.open-in-view=false`, JJWT/Kakao 테스트 시크릿. `flyway_schema_history` 행 ≥ 2 그린 ✓
- [x] **0.5** Singleton container 패턴 — `static final MySQLContainer<?> MYSQL` 정적 필드, JVM 1회 기동. `FlywayMigrationIntegrationTest` 2개 테스트가 동일 컨테이너 공유 ✓

### 추가 (V3-5 격리정책 박제)

- [x] **0.6** 격리 정책 docs (ADR-001에 박제) — `AbstractIntegrationTest` javadoc 에 정책 명시 + `99-adr/ADR-001-testcontainers-isolation.md` 본문 박제 완료 ✓

---

## [필수 4] 검증 명령 + README 게이트

검증 명령:
```
./gradlew build
./gradlew test --tests "gift.ApplicationTest"
./gradlew test
```

게이트 (V3-C):
- [ ] 체크박스 3요소 캡처 (PR description)
- [ ] README 진행률 `[ ]` → `[x]` 갱신 (PR #1)
- [ ] 본 문서 §변경 로그 1줄 추가

---

## [선택 1] 작동 변경 vs 구조 변경 표시

| 변경 ID | 종류 | 관련 ADR |
|---|---|---|
| 0.1~0.6 | 구조 (인프라 추가) | ADR-001 |

## [선택 3] 관련 ADR

- [ADR-001](./99-adr/ADR-001-testcontainers-isolation.md) — Singleton MySQLContainer + `@ServiceConnection` + 격리정책

## [선택 4] 후속 작업

- CI 캐싱 전략 (별도 ADR 후보)
- Gradle 테스트 병렬화 — 선택 사항, 현 사이클 강제 안 함

## [선택 5] 변경 로그

- 2026-05-16: 0.1~0.6 모두 완료. `./gradlew test` 그린 (3 tests / 0 failed / 0 skipped — ApplicationTest 1건 + FlywayMigrationIntegrationTest 2건). `./gradlew ktlintCheck` 그린.
- Testcontainers Docker 의존성 확인: Docker Desktop 가동 필요. Java toolchain: Java 21 (Corretto) 권장. Gradle 8.14 가 Java 25 launcher 미지원이므로 `JAVA_HOME=/Users/.../corretto-21.0.9/Contents/Home` 명시 필요.
