# ADR-001 · Testcontainers + 격리정책 + AFTER_COMMIT 함정

## Status
Accepted (2026-05-16)

## Context
- src/test 가 비어있어(.gitkeep만) **회귀 안전망 0**.
- prd 라인 23-24: "테스트 환경은 테스트 컨테이너를 사용한다" — 강제.
- 운영 DB는 MySQL, 런타임은 H2 — JPA 동작 차이 위험. 통합 테스트는 운영 동등성 필요.
- Spring Boot 3.1+ 에서 `@ServiceConnection` 으로 datasource 수동 오버라이드 제거 가능.

## Decision

### 1) 컨테이너 전략
- **Singleton `MySQLContainer<?>`** (`@Container static` 필드, JUnit5 `@BeforeAll`) — 테스트 스위트 전체에서 1회만 시작.
- **`@ServiceConnection`** (Spring Boot 3.1+) 사용 — datasource URL 자동 주입, `application-test.properties` 의 수동 오버라이드 제거.
- 기반 추상 클래스: `gift.support.AbstractIntegrationTest` (`@SpringBootTest` + `@ActiveProfiles("test")` + `@Testcontainers`).

### 2) 격리 정책 매트릭스

| 테스트 유형 | 트랜잭션 | 정리 방식 |
|---|---|---|
| 단위 (도메인/서비스 Mock) | 해당 없음 | — |
| Repository (`@DataJpaTest`) | 기본 `@Transactional` rollback | 자동 |
| Service 통합 (`@SpringBootTest`) | 기본 `@Transactional` rollback | 자동 |
| **AFTER_COMMIT 이벤트 검증** | **`@Transactional` 미부착** | `@Sql(scripts="/cleanup.sql", executionPhase=AFTER_TEST_METHOD)` |
| WebMvcTest 스냅샷 | 해당 없음 | — |

### 3) AFTER_COMMIT 함정 박제 (V3-5)

> **함정**: Spring `@Transactional` 테스트 안에서는 테스트 종료 시 트랜잭션이 **롤백**된다. `@TransactionalEventListener(phase=AFTER_COMMIT)` 는 커밋 후 발화하므로 **트랜잭션 안 테스트에서는 이벤트가 발화하지 않는다**. False-green 위험.
>
> **회피**: AFTER_COMMIT 이벤트 발화 검증 테스트는
> - `@Transactional` **부착 금지**
> - DB 정리: `@Sql(scripts="...cleanup.sql", executionPhase=AFTER_TEST_METHOD)`
> - 이벤트 발화 검증: `@RecordApplicationEvents` + `ApplicationEvents.stream(SomeEvent.class)`
>
> 적용 위치: 06-order Phase B.2 (`KakaoNotificationListener` 발화 검증 테스트).

### 4) 의존성
`build.gradle.kts`:
```
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:junit-jupiter")
testImplementation("org.testcontainers:mysql")
```

## Drivers
- 운영-테스트 동등성 (prd 라인 23)
- 회귀 안전망 확보 (현재 0)
- AFTER_COMMIT 이벤트 검증 정확성

## Alternatives considered
- **H2 in-memory 유지**: JPA 방언 차이, AFTER_COMMIT 이벤트 정확도 떨어짐. 탈락.
- **Embedded MySQL**: 유지보수 부담, Docker 의존 회피 가치가 학습 환경에서 작음. 탈락.
- **Container per test class**: 격리 우수하지만 CI 시간 폭증 (도메인당 클래스 ≥3 × 컨테이너 기동 ≈ 분 단위). 탈락 → Singleton 채택.

## Why chosen
- Singleton + `@ServiceConnection` = 최소 CI 시간 + 운영 동등성 + Spring Boot 표준.
- 격리 매트릭스 + AFTER_COMMIT 함정 박제 = 06-order Phase B.2 false-green 차단.

## Consequences
- 첫 통합 테스트 실행 느림(컨테이너 풀링) — 이후 재사용으로 amortize.
- 도커 의존성 명시 (학습 환경 전제).
- `@Sql` cleanup 파일 1~2개 필요 (06-order 한정).

## Follow-ups
- CI 캐싱 전략 (별도 ADR 후보).
- Gradle 테스트 병렬화 — **선택 사항**, 본 사이클 강제 없음.
- ADR-005 (Testcontainers 별도 ADR) 폐기, 본 ADR로 흡수.

## 적용 PR
- 00-test-infra (전제 도입)
- 06-order Phase B.2 (AFTER_COMMIT 검증 테스트)
