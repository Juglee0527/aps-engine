# APS Engine Project Definition

## 1. 프로젝트 목적

`aps-engine`은 제조 현장의 생산계획 문제를 이해하고 해결하기 위해 APS(Advanced Planning & Scheduling)의 핵심 기능을 단계적으로 구현하는 오픈소스 학습·포트폴리오 프로젝트입니다.

단순 CRUD 예제가 아니라 다음 질문에 답할 수 있는 실행 가능한 시스템을 목표로 합니다.

- 어떤 생산오더를 언제 시작해야 하는가?
- 어떤 설비와 생산라인에 작업을 배정해야 하는가?
- 주어진 가용시간 안에서 납기와 우선순위를 어떻게 반영할 것인가?
- CAPA 부족과 병목 설비를 어떻게 발견할 것인가?
- 정비, 휴무, Changeover Time 같은 제조 제약을 일정에 어떻게 반영할 것인가?
- 여러 설비와 우선순위 정책 중 어떤 계획이 제조 KPI에 더 유리한가?
- 대량 제조 데이터를 안정적으로 처리하고 같은 조건의 계획을 재현할 수 있는가?

최종 목표는 프로젝트마다 다시 작성하는 일회성 스케줄러가 아니라, 입력 데이터와 정책을 바꿔 재사용할 수 있는 표준 알고리즘 엔진입니다.
코드 품질뿐 아니라 설계 근거, 테스트 전략, 제조 KPI와 성능 변화를 문서로 남겨 APS 백엔드 개발 역량을 설명할 수 있는 수준을 지향합니다.

## 2. 기술 기준

| 구분 | 현재 적용 | 목표 또는 적용 예정 |
| --- | --- | --- |
| Language | Java 21 | - |
| Framework | Spring Boot 3.5.16 | - |
| Build | Gradle 8.14.4 Wrapper | - |
| Database | PostgreSQL 18.4 | - |
| ORM | Spring Data JPA | QueryDSL |
| Migration | Flyway | - |
| Cache | 미적용, 측정 근거 부족으로 보류 | Redis 재검토 |
| Test | JUnit 5, Mockito, Spring Boot Test, Testcontainers | - |
| Container | PostgreSQL + APS Engine Docker Compose | - |
| CI | GitHub Actions Java 21 Gradle build | - |

QueryDSL은 아직 적용 전입니다. Redis는 구조 검토 결과 도입을 보류했고,
Testcontainers, 애플리케이션 Docker 이미지와 GitHub Actions는 운영 경계를 확인해 적용했습니다.

기본 Java 패키지는 저장소 소유자와 프로젝트명을 기준으로 다음 값을 사용합니다.

```text
com.github.juglee0527.apsengine
```

현재 `build.gradle`은 Spring Boot 3.5.16과 Java 21 toolchain을 명시적으로 고정합니다.

## 3. 핵심 용어

| 용어 | 이 프로젝트에서의 의미 |
| --- | --- |
| APS | 생산 자원과 제약조건을 반영해 실행 가능한 생산계획을 생성하는 시스템 |
| Factory | 생산라인과 설비가 소속되는 공장 |
| Production Line | 공장 안에서 제품 생산이 이루어지는 논리적 자원 그룹 |
| Machine | 실제 작업을 수행하며 가용시간과 CAPA를 갖는 설비 |
| Product | 생산오더가 요구하는 생산 대상 품목 |
| Routing | 제품 생산에 필요한 Operation의 순서 |
| Operation | 특정 설비에서 수행되는 하나의 공정 단계 |
| Production Order | 제품, 수량, 납기일 및 우선순위를 가진 생산 지시 |
| Working Calendar | 설비가 작업할 수 있는 날짜와 시간 구간 |
| CAPA | 일정 기간에 설비가 제공할 수 있는 생산 능력 |
| Constraint | 작업 배치를 제한하는 가용시간, 정비, 선후관계 등의 조건 |
| Changeover Time | 설비에서 생산 제품이 바뀔 때 추가로 필요한 준비시간 |
| Lead Time | 생산오더의 시작부터 완료까지 걸리는 전체 계획 시간 |
| Priority Rule | 여러 생산오더의 처리 순서를 결정하는 규칙 |
| Bottleneck | 부하가 집중되어 전체 생산 흐름을 제한하는 자원 |
| Schedule | 작업별 설비, 시작 시각, 종료 시각을 포함한 계획 결과 |

용어는 구현 과정에서 의미가 달라지지 않도록 코드, API 및 문서에서 동일하게 사용합니다.

## 4. 구현 범위

개발은 [커밋 단위 로드맵](01-commit-roadmap.md)에 정의된 순서로 진행합니다.

### 프로젝트 기반

- Spring Boot 및 Gradle 프로젝트
- PostgreSQL 연동과 스키마 마이그레이션
- 공통 API 오류 응답

### 로컬 PostgreSQL 연결

로컬 환경은 `compose.yml`의 PostgreSQL 단일 서비스로 구성합니다.

```text
Spring Boot(local profile)
  → jdbc:postgresql://localhost:${POSTGRES_PORT}/${POSTGRES_DB}
  → PostgreSQL 18.4 container
  → postgres-data named volume
```

- 사용자명, 비밀번호, 데이터베이스명과 호스트 포트는 환경변수로 주입합니다.
- `POSTGRES_PASSWORD`는 필수값이며 Compose 파일에 기본 비밀번호를 두지 않습니다.
- `.env.example`은 변수명과 예시만 제공하고 실제 `.env`는 Git에서 제외합니다.
- 컨테이너 내부 데이터는 `postgres-data` 이름의 볼륨에 보존합니다.
- 애플리케이션은 `local` 프로필에서만 로컬 DataSource를 구성합니다.
- JPA는 스키마를 자동 생성하지 않고 `ddl-auto=validate`로 코드와 스키마의 일치 여부만 검사합니다.

### 데이터베이스 마이그레이션

스키마 변경 도구는 Flyway를 사용합니다.

- 현재 스키마 변경은 SQL로 명확하게 검토할 수 있으므로 별도 포맷 계층이 필요한 Liquibase보다 단순합니다.
- 마이그레이션 파일은 `src/main/resources/db/migration`에 `V<버전>__<설명>.sql` 형식으로 추가합니다.
- `V1__baseline.sql`은 도메인 테이블이 없는 초기 기준점이며 Flyway 연동 자체를 검증합니다.
- 한번 적용되어 공유된 마이그레이션은 수정하지 않고 새로운 버전 파일로 변경합니다.
- Flyway가 애플리케이션 시작 시 먼저 스키마를 변경하고 Hibernate는 `validate`만 수행합니다.
- `baseline-on-migrate`는 기존 비관리 스키마를 암묵적으로 수용하지 않도록 `false`로 유지합니다.

### 생산 기준정보

- Factory
- Production Line
- Machine
- Product
- Routing과 Operation

### 생산오더

- ProductionOrder 등록과 조회
- 명시적인 상태 전이
- 납기일과 우선순위

### CAPA와 스케줄링

- Working Calendar 기반 설비 가용시간
- 설비 CAPA와 사용률
- 단일 설비에서 시작하는 순방향 스케줄링
- 다중 Operation 및 다중 ProductionOrder 처리
- Priority Rule
- 스케줄 결과 저장
- 실행 당시 고정 UTC offset 보존
- 실제 스케줄 결과 기반 설비별 간트와 CAPA 사용률 시각화

### 제조 제약조건

- Changeover Time
- Maintenance
- Lead Time
- Bottleneck Detection

### 운영 기반

- 필요한 조회 경로에 한정한 Redis 캐시
- Testcontainers 통합 테스트
- Docker 이미지와 Docker Compose
- GitHub Actions
- 측정 기반 성능 개선

### APS 엔진 고도화

- Operation 대체 설비와 결정론적 설비 선택
- EDD, SPT 등 Dispatching Rule 비교
- 총 지연시간, Makespan, 설비 가동률 등 계획 KPI
- 기존 계획을 일부 고정하는 재스케줄링과 Frozen Horizon

### 데이터 처리와 실행 운영

- CSV 기반 기준정보·생산오더 대량 입력
- 행 단위 검증, 오류 리포트와 중복 요청 멱등성
- 비동기 스케줄 실행과 실행 상태·이력 조회
- 실행시간, 메모리, DB 쿼리와 실패 원인을 확인하는 관측성

## 5. 현재 제외 범위

다음 기능은 현재 `001~047` 핵심 로드맵에 포함하지 않습니다.

- 수요예측과 판매계획
- MRP와 원자재 재고 가용성
- BOM 및 대체 자재
- 구매오더와 공급업체 관리
- 인력과 작업자 스킬 기반 배정
- 운송 및 물류 최적화
- 다공장 간 물량 배분
- 실시간 MES 설비 데이터 수집
- 상용 Solver 또는 AI 기반 최적화
- 멀티테넌시, 사용자 관리 및 인증·인가
- 운영 배포와 클라우드 인프라

제외 기능은 명시적인 로드맵 변경 없이 미리 구현하지 않습니다.

## 6. 현재 구현 스냅샷

2026년 7월 30일 기준 로드맵 `001~042`와 APS Schedule Control Tower가 구현되어 있습니다.

| 영역 | 구현 상태 |
| --- | --- |
| 생산 자원 | Factory, ProductionLine, Machine 등록·조회 |
| 품목과 공정 | Product, Routing, Operation 등록·조회, 후보 설비와 우선순위 정의 |
| 생산오더 | 등록·조회, DRAFT → CONFIRMED → SCHEDULED |
| CAPA | WorkingCalendar, 가용 구간·가용 분, 병목 후보 진단 |
| 스케줄링 | 대체 설비 순방향 배정, Changeover·Maintenance, 명시적 우선순위·EDD·SPT |
| 결과 | ScheduleRun 규칙·KPI 스냅샷, 가공·Changeover·대기시간별 계획 Lead Time |
| 제조 제약 | 방향성 Changeover Time과 특정 날짜의 계획 정비시간 |
| 화면 | 오더 큐, 작업·Changeover 간트, 납기 지연, 병목 후보, 기준정보 등록 |
| 캐시 | 호출량·지연 근거 부족으로 Redis 도입 보류, 재검토 기준 문서화 |
| 통합 테스트 | PostgreSQL Testcontainers 공통 기반과 Factory Repository 검증 |
| 실행 환경 | 비루트 멀티 스테이지 Docker 이미지와 PostgreSQL Compose |
| CI | push·pull request Java 21 Gradle 테스트와 결과 artifact |
| 성능 | 소·중·대 ForwardScheduler 기준선, JFR 프로파일링과 빈 비가용 구간 빠른 경로 |

다음 구현 대상은 `043. Frozen Horizon 재스케줄링`입니다. 운영 기반 `034~039`와
대체 설비·규칙 비교 `040~042`는 완료했고, APS 엔진 고도화 `043`, 데이터 처리와
실행 운영 `044~047`은 아직 구현되지 않았습니다.

## 7. 아키텍처 경계

Layered Architecture를 기본으로 사용하며 기능별 패키지 안에서 다음 책임을 구분합니다.

```text
API 요청
  → Controller: HTTP 입력 검증과 응답 변환
  → Application Service: 유스케이스와 트랜잭션 경계
  → Domain: 상태, 규칙, 불변조건
  → Repository/Infrastructure: 영속화와 외부 기술 연동
```

Clean Architecture와 DDD의 개념은 도메인 규칙을 보호하는 데 필요한 만큼만 적용합니다. 현재 요구사항에 필요하지 않은 Port, Adapter, 추상 Repository 또는 범용 프레임워크는 만들지 않습니다.

## 8. 개발 완료 기준

각 커밋 단위는 다음 조건을 모두 만족해야 완료로 판단합니다.

- 요청된 범위만 구현되어 있습니다.
- 해결하려는 제조 문제와 기대 KPI가 명시되어 있습니다.
- 설계 선택과 제외 범위가 설명되어 있습니다.
- 실행 가능한 코드와 관련 테스트가 있습니다.
- 정상, 경계값 및 오류 케이스가 검토되었습니다.
- 관련 문서와 README가 실제 구현 상태를 반영합니다.
- Gradle 테스트가 통과하거나 환경상 미실행 사유가 기록되어 있습니다.
- 하나의 Conventional Commit으로 커밋되어 원격 브랜치에 push되었습니다.
- 로드맵 체크박스가 완료 상태로 변경되었습니다.

## 9. 기존 문서 정합성 검토

001 단계에서 확인한 문서 차이는 다음과 같이 정리합니다.

| 항목 | 기존 README | 개발 지침 및 확정 방향 |
| --- | --- | --- |
| 단계 번호 | README는 프로젝트 기반을 Phase 1로 시작 | 커밋 로드맵은 프로젝트 기반을 Phase 0으로 정의 |
| 아키텍처 | DDD와 Clean Architecture를 원칙으로 단순 표기 | Layered Architecture를 기본으로 하고 필요한 개념만 제한적으로 적용 |
| Redis 시점 | 초기 구성과 성능 단계에 모두 표시 | 실제 필요성이 확인되는 034 단계에서 적용 여부 결정 |
| 범위 | Work Center, Sales Order, Material Availability, Load Balancing 포함 | 현재 `001~047` 핵심 로드맵에서는 제외 |
| 진행상태 | 포괄적인 `In Progress`만 표시 | 번호별 체크박스와 원격 push 여부로 추적 |

이 문서와 커밋 단위 로드맵을 상세 범위의 기준으로 사용합니다. README는 프로젝트 소개와 현재 상태를 간결하게 보여주는 진입점으로 유지합니다.
