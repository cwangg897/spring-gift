# 00-test-infra · 테스트 인프라 (PR #1, 완료)

> **상태**: ✅ 완료 (2026-05-16, 커밋 `9ecf8c6`)

---

## 1. 현재 상태 진단 (작업 시작 시점 기록)

- `src/test/java/gift/.gitkeep`, `src/test/kotlin/gift/.gitkeep` — 테스트 0개
- `build.gradle.kts:39-41` — `spring-boot-starter-test` 만, Testcontainers 의존성 부재
- 테스트 프로필 분리 부재
- Flyway V1/V2 (`src/main/resources/db/migration/`) 존재, 재사용 대상

---

## 2. 산출물 + 체크리스트

- [x] `build.gradle.kts:40-42` Testcontainers 의존성 3개 추가 (`spring-boot-testcontainers`, `testcontainers:junit-jupiter`, `testcontainers:mysql`)
- [x] `src/test/java/gift/ApplicationTest.java` — `@SpringBootTest` + `@ActiveProfiles("test")` + `contextLoads()`
- [x] `src/test/java/gift/support/AbstractIntegrationTest.java` — Singleton `MySQLContainer<?>` + `@ServiceConnection` + `@Testcontainers`, 격리 정책 javadoc 박제
- [x] `src/test/java/gift/support/FlywayMigrationIntegrationTest.java` — Flyway V1/V2 적용 검증 (카나리)
- [x] `src/test/resources/application-test.properties` — `flyway.enabled=true`, `ddl-auto=validate`, JJWT/Kakao 테스트 시크릿

---

## 3. 검증 명령

```
JAVA_HOME=/Users/.../corretto-21 ./gradlew test
```

결과: **3 passed / 0 failed / 0 skipped** ✓
- `ApplicationTest.contextLoads` (H2)
- `FlywayMigrationIntegrationTest.flywayHistoryContainsV1AndV2` (Testcontainers MySQL 8)
- `FlywayMigrationIntegrationTest.seedDataFromV2IsPresent` (Testcontainers MySQL 8)

### 환경 요구
- Java 21 launcher (Gradle 8.14 가 Java 25 launcher 미지원)
- Docker Desktop 가동 (Testcontainers)

---

## 4. 변경 로그

- 2026-05-16: 모든 항목 완료, 커밋 `9ecf8c6` (`feat(test-infra): add Testcontainers MySQL + ApplicationTest`). Architect APPROVE.
