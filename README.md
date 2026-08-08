# APS Engine

> Production Scheduling & Planning Engine built with Spring Boot

APS Engine은 제조업의 생산계획(APS, Advanced Planning & Scheduling)의 핵심 기능을 직접 구현하며 학습하는 프로젝트입니다.

단순 CRUD 프로젝트가 아니라 실제 제조 현장에서 사용하는 APS의 생산계획, 설비(Capacity), 작업 스케줄링, 제약조건(Constraint), 최적화 로직을 단계적으로 구현하는 것을 목표로 합니다.

---

# 🚀 로컬 서버 바로 실행

Java 21과 Docker Desktop만 준비하고 Docker Desktop을 실행해 주세요.
별도의 Gradle 및 PostgreSQL 설치는 필요하지 않습니다.

## PowerShell에 복사해서 실행

아래 블록을 한 번에 복사해 PowerShell에 붙여넣습니다.

```powershell
Set-Location C:\Users\user\IdeaProjects\aps-engine
.\scripts\run-local.ps1
```

## 실행 파일을 눌러서 실행

Windows 파일 탐색기에서 저장소 루트의 [`run-local.cmd`](run-local.cmd)를 더블클릭합니다.

실행 스크립트가 다음 작업을 자동으로 처리합니다.

1. `.env`가 없으면 `.env.example`을 복사해 생성합니다.
2. Docker PostgreSQL을 시작하고 `healthy` 상태까지 기다립니다.
3. `8080` 포트가 사용 중이면 `8081`부터 사용 가능한 포트를 선택합니다.
4. 선택한 접속 주소를 출력하고 Spring Boot 서버를 실행합니다.

터미널에 출력된 주소로 접속합니다.

```text
[APS] 서버 주소: http://localhost:8080
```

서버를 종료하려면 실행 중인 터미널에서 `Ctrl+C`를 누릅니다.
상세 설정과 문제 해결 방법은 [Local Development](#local-development)를 참고해 주세요.

## 애플리케이션까지 Docker로 실행

`.env`를 준비한 뒤 PostgreSQL과 APS Engine 이미지를 함께 빌드·실행합니다.

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

기본 접속 주소는 `http://localhost:8080`, 상태 확인 주소는
`http://localhost:8080/actuator/health`, 메트릭 목록은
`http://localhost:8080/actuator/metrics`입니다. 상세 환경변수와 종료 방법은
[Docker 애플리케이션 이미지](docs/10-docker.md)를 참고해 주세요.

---

# Goals

- APS 도메인 이해
- 생산계획(Scheduling) 알고리즘 구현
- 설비 CAPA(Capacity Planning) 계산
- 생산 제약조건(Constraint) 처리
- 재사용 가능한 스케줄링 정책과 제조 KPI 비교
- 대량 제조 데이터 처리와 실행 이력 관리
- 성능 최적화
- 테스트 코드 및 CI/CD 구축
- 실무 수준의 Spring Boot 프로젝트 설계

---

# Current Implementation

2026년 8월 8일 기준으로 핵심 APS 엔진 `001~047`, 학습 경험 `048~059`와
보조 MVP 단위 `011-A`, `011-B`를 완료했습니다. 표준 Gradle 테스트는 302개 중 283개 통과,
환경 조건부 19개 스킵, 별도 성능 테스트 2개 통과입니다.

```text
Factory → ProductionLine → Machine → WorkingCalendar
Product → Routing → Operation
                    └→ OperationMachineCandidate → Machine
ProductionOrder → ForwardScheduler → ScheduleRun → ScheduledOperation
Machine + Product 전환 방향 → ChangeoverTime
```

- 명시적 우선순위, 납기와 식별자를 적용하는 순방향 스케줄러
- 설비 근무시간과 공정 선후관계를 반영한 유한 CAPA 배정
- 방향성 Changeover Time을 설비 근무시간 안에서 배정하고 결과에 저장
- 계획 정비시간을 CAPA와 스케줄 가용시간에서 제외
- 저장된 스케줄의 가공·Changeover·대기시간별 계획 Lead Time 계산
- 설비 CAPA 사용률 80% 기준 병목 후보 순위와 진단 사유 제공
- 실행 키 기반 중복 방지와 스케줄 결과 원자적 저장
- 설비·이전 품목·다음 품목별 방향성 Changeover Time 기준정보
- 실제 API 데이터 기반 작업·Changeover 간트, 납기 지연 및 병목 후보 화면
- Redis 캐시는 반복 호출량과 성능 근거가 없어 현재 도입 보류
- Docker 사용 가능 시 PostgreSQL 컨테이너에서 Flyway·Repository 통합 검증
- 비루트 멀티 스테이지 애플리케이션 이미지와 PostgreSQL Compose 실행
- push·pull request에서 Java 21 Gradle 테스트와 결과 보존
- 소·중·대 입력을 분리 실행하는 ForwardScheduler 성능 기준선
- JFR로 확인한 빈 비가용 구간 정규화 비용 제거와 성능 회귀 테스트
- 기존 주 설비 계약과 호환되는 Operation 후보 설비·우선순위 모델
- 완료시각·후보 우선순위·설비 ID 기반 결정론적 대체 설비 선택
- 명시적 우선순위·EDD·SPT 실행 선택과 지연·Makespan·설비 가동률 KPI 스냅샷
- 시작·진행 작업을 유지하고 미래 작업·신규 확정 오더만 재배치하는 Frozen Horizon
- UTF-8 CSV 샘플·파일 제한·참조 순서·행별 오류를 제공하는 DB 무변경 미리보기
- 요청 키·파일 해시 기반 중복 방지, 원자적 CSV 반영, 행별 결과 이력과 중단 재시도
- 단일 내부 작업자 기반 비동기 스케줄 큐, 상태·실패 이력 조회와 재시작 복구
- Actuator·Micrometer 기반 실행시간·입력 규모·생성 작업·실패 단계 메트릭과 구조화 로그
- 12개 결정론적 APS 실습, 규칙·제약·Frozen Horizon 전후 비교와 결과 코치
- 150·600오더 학습 데이터, 서버 페이지·검색·설비·기간별 간트 탐색
- 계정 없이 복원되는 브라우저 학습 진도와 초기화 데이터 재확인

# Tech Stack

## Applied

| 구분 | 현재 적용 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.16, Gradle 8.14.4 |
| Persistence | Spring Data JPA, Hibernate, Flyway |
| Database | PostgreSQL 18.4 |
| Test | JUnit 5, Mockito, Spring Boot Test, Testcontainers |
| Runtime | Multi-stage Docker image, Spring Boot Actuator, Micrometer |
| Local Environment | PostgreSQL + APS Engine Docker Compose |
| CI | GitHub Actions, Gradle Wrapper validation and cache |
| UI | Spring Boot Static Resources, Vanilla JavaScript |

## Planned

QueryDSL은 아직 적용하지 않았습니다. Redis는 측정 근거가 없어 도입을 보류했습니다.

---

# Roadmap

`001~047`의 핵심 단위와 `011-A`, `011-B` 보조 MVP 단위는 모두 완료했습니다.
APS를 설명하고 직접 실험할 수 있는 학습 경험 로드맵 `048~059`까지 완료했습니다.

- Phase 0: 프로젝트 기반
- Phase 1: 공장과 생산 자원
- Phase 2: 제품과 공정
- Phase 3: 생산오더
- Phase 4: CAPA
- Phase 5: 기본 스케줄링
- Phase 6: 제조 제약조건
- Phase 7: 운영 기반
- Phase 8: APS 엔진 고도화
- Phase 9: 데이터 처리와 실행 운영
- Phase 10: APS 학습 경험

상세 범위와 완료 이력은 [커밋 단위 개발 로드맵](docs/01-commit-roadmap.md)에서 관리합니다.
새 기능은 실제 사용·측정 결과와 명시적인 요구사항을 근거로 별도 커밋 단위를 정의한 뒤 진행합니다.

---

# Project Structure

```
aps-engine
├── docs
├── gradle
├── scripts
├── src
│   ├── main
│   └── test
├── compose.yml
├── README.md
└── build.gradle
```

---

# Documents

프로젝트 진행 과정은 문서로 함께 관리합니다.

- [개발 지침](docs/00-development-guidelines.md)
- [커밋 단위 개발 로드맵](docs/01-commit-roadmap.md)
- [프로젝트 목표와 범위](docs/01-project.md)
- [도메인 모델](docs/02-domain.md)
- [ERD](docs/03-erd.md)
- [API 계약](docs/04-api.md)
- [APS Schedule Control Tower](docs/05-mvp-ui.md)
- [순방향 스케줄링](docs/05-scheduling.md)
- [CAPA 계산](docs/06-capacity.md)
- [스케줄링 성능 기준선](docs/07-performance.md)
- [캐시 도입 판단](docs/08-cache-strategy.md)
- [PostgreSQL Testcontainers](docs/09-testcontainers.md)
- [Docker 애플리케이션 이미지](docs/10-docker.md)
- [GitHub Actions 빌드](docs/11-ci.md)
- [계획 데이터 CSV 입력](docs/12-csv-import.md)
- [비동기 스케줄 실행](docs/13-async-scheduling.md)
- [스케줄 실행 관측성](docs/14-observability.md)
- [APS 학습 가이드 개편 명세](docs/15-aps-learning-guide.md)

---

# Development Principles

- Layered Architecture
- Domain-Centered Design
- Testable Code
- Object-Oriented Programming
- SOLID Principles
- Small and Verifiable Changes

---

# Current Status

✅ Core Roadmap Complete

✅ APS Learning Roadmap Complete

- [x] 001. 프로젝트 요구사항과 범위 문서화
- [x] 002. Spring Boot 프로젝트 초기화
- [x] 003. 로컬 PostgreSQL 환경 구성
- [x] 004. 데이터베이스 마이그레이션 기반 추가
- [x] 005. 공통 API 오류 응답 구성
- [x] 006. Factory 도메인 모델
- [x] 007. Factory 등록 API
- [x] 008. Factory 조회 API
- [x] 009. ProductionLine 도메인과 등록 API
- [x] 010. Machine 도메인 모델
- [x] 011. Machine 등록 및 조회 API
- [x] 011-A. MVP용 ProductionLine 목록 조회 API
- [x] 011-B. APS 운영 화면 MVP
- [x] 012. Product 도메인과 등록 API
- [x] 013. Product 조회 API
- [x] 014. Routing과 Operation 도메인 모델
- [x] 015. Routing 등록 및 조회 API
- [x] 016. ProductionOrder 도메인 모델
- [x] 017. ProductionOrder 등록 API
- [x] 018. ProductionOrder 조회 API
- [x] 019. ProductionOrder 상태 변경
- [x] 020. WorkingCalendar 도메인 모델
- [x] 021. 설비 가용시간 조회
- [x] 022. 설비 CAPA 계산
- [x] 023. Scheduling 입력과 결과 모델
- [x] 024. 단일 설비 순방향 스케줄링
- [x] 025. 다중 Operation 스케줄링
- [x] 026. 다중 생산오더 우선순위 규칙
- [x] 027. 스케줄 실행 유스케이스
- [x] 028. 스케줄 결과 저장
- [x] 029. Changeover Time 모델
- [x] 030. 스케줄러에 Changeover Time 적용
- [x] 031. Maintenance 제약조건
- [x] 032. Lead Time 계산
- [x] 033. Bottleneck 탐지
- [x] 034. Redis 캐시 적용 대상 검증
- [x] 035. Testcontainers 통합 테스트 기반
- [x] 036. Docker 애플리케이션 이미지
- [x] 037. GitHub Actions 빌드 검증
- [x] 038. 스케줄링 성능 기준선
- [x] 039. 측정 기반 성능 개선
- [x] 040. Operation 대체 설비 모델
- [x] 041. 결정론적 대체 설비 선택
- [x] 042. Dispatching Rule과 계획 KPI 비교
- [x] 043. Frozen Horizon 재스케줄링
- [x] 044. CSV 대량 입력 검증과 미리보기
- [x] 045. 대량 입력 멱등성과 실패 복구
- [x] 046. 비동기 스케줄 실행과 이력 조회
- [x] 047. 스케줄 실행 관측성
- [x] 048. APS 학습 경험 명세
- [x] 049. 프론트엔드 모듈화
- [x] 050. 학습 가이드 정보구조 개편
- [x] 051. 학습 시나리오 모델
- [x] 052. 학습 시나리오 계획 범위
- [x] 053. 기초 APS 학습 시나리오
- [x] 054. Dispatching Rule 비교 실험실
- [x] 055. 제조 제약 학습 시나리오
- [x] 056. Frozen Horizon 학습 시나리오
- [x] 057. 대량 시나리오와 계획 탐색
- [x] 058. 학습 진도와 결과 코치
- [x] 059. APS 학습 경험 통합 검증

2026년 8월 8일 `cleanTest test`를 실행해 표준 테스트 302개 중 283개 통과, 실패 0개,
Docker 또는 별도 환경이 필요한 조건부 테스트 19개 스킵을 확인했습니다. 별도
`performanceTest` 2개도 실패 없이 통과했으며 600오더 학습 기준과 100~5,000오더 엔진 기준을
분리해 검증합니다.
상세 완료 이력은 [커밋 단위 개발 로드맵](docs/01-commit-roadmap.md)을 참고해 주세요.

---

# Local Development

## Requirements

- Java 21
- Docker Desktop
- PowerShell

별도의 Gradle 설치는 필요하지 않습니다. 저장소에 포함된 Gradle Wrapper를 사용합니다.
아래 명령어는 저장소 루트인 `C:\Users\user\IdeaProjects\aps-engine`에서 실행합니다.

## Quick Start

```powershell
.\scripts\run-local.ps1
```

스크립트는 `.env` 준비, PostgreSQL 실행과 상태 확인, 서버 포트 선택 및 Spring Boot 실행을 한 번에 처리합니다.
`8080`이 사용 중이면 `8081`부터 순서대로 빈 포트를 찾아 실제 접속 주소를 터미널에 출력합니다.

특정 포트를 사용하려면 다음과 같이 실행합니다. 해당 포트가 사용 중이면 다음 빈 포트를 자동으로 선택합니다.

```powershell
.\scripts\run-local.ps1 -ServerPort 9090
```

## Environment

최초 실행 시 `.env`가 없으면 `.env.example`을 자동으로 복사합니다.
필요하면 서버 실행 전에 `.env`에서 로컬 PostgreSQL 설정을 변경합니다.

```dotenv
POSTGRES_DB=aps
POSTGRES_USER=aps
POSTGRES_PASSWORD=<로컬에서 사용할 비밀번호>
POSTGRES_PORT=5432
APP_PORT=8080
```

`.env`는 Git 추적 대상에서 제외됩니다. 실제 비밀번호를 `.env.example`이나 소스 코드에 기록하지 마세요.

`run-local.ps1`은 다음 작업을 수행합니다.

1. `.env`의 PostgreSQL 연결 정보를 현재 프로세스에 적용합니다.
2. Docker Desktop과 PostgreSQL 컨테이너 상태를 확인합니다.
3. PostgreSQL이 `healthy`가 될 때까지 최대 60초 기다립니다.
4. 사용할 수 있는 서버 포트를 선택합니다.
5. 저장소 내부의 `.gradle-user-home`을 Gradle 캐시로 사용합니다.
6. `local` Spring Profile로 애플리케이션을 실행합니다.
7. Flyway 마이그레이션을 적용하고 Hibernate 매핑을 검증합니다.

첫 실행에서는 Gradle 8.14.4와 의존성을 다운로드하므로 시간이 걸릴 수 있습니다.

## Open

서버 시작 로그에서 `Started ApsEngineApplication`을 확인한 다음 브라우저에서 접속합니다.
실제 주소는 `[APS] 서버 주소:` 다음에 출력되며, 포트 상황에 따라 `8080`, `8081` 등이 될 수 있습니다.

왼쪽 아래 `ENGINE STATUS`가 `API ONLINE`이면 정상적으로 실행된 상태입니다.

처음 사용하는 경우 화면의 `사용자 가이드` 메뉴에서 APS 개념과 과정 A~F를 읽고 12개 실습을
바로 실행할 수 있습니다. 첫 생산계획은 다음 4단계 샘플 흐름으로도 만들 수 있으며, 각 단계는
완료 후 다음 단계가 활성화되고 중간에 실패해도 이미 등록된 데이터부터 이어집니다.

```text
[1] 샘플 공장 · 라인 · 설비 · 근무시간 등록
  → [2] 샘플 품목 · Routing · Operation 등록
    → [3] 우선순위가 다른 생산오더 2건 등록 · 확정
      → [4] 스케줄 실행 · 설비별 간트 · 납기 지연 · CAPA 확인
```

기존 데이터를 수정하거나 삭제하지 않으며, 직접 입력하려는 경우 기존 마스터 데이터와
생산오더 등록 화면도 그대로 사용할 수 있습니다.

## Stop

Spring Boot가 실행 중인 터미널에서 `Ctrl+C`를 누른 뒤 PostgreSQL을 중지합니다.

```powershell
docker compose stop postgres
```

PostgreSQL 데이터는 `postgres-data` Docker Volume에 보존되므로 다음 실행에서도 유지됩니다.

## Troubleshooting

### `.env 파일이 없습니다`

저장소 루트에서 다음 명령어를 실행합니다.

```powershell
Copy-Item .env.example .env
```

### `POSTGRES_PASSWORD is required`

`.env`의 `POSTGRES_PASSWORD`가 비어 있지 않은지 확인합니다.

### `password authentication failed for user "aps"`

현재 `.env`의 비밀번호와 기존 `postgres-data` Volume을 처음 만들 때 사용한 비밀번호가 다릅니다.
기존 데이터가 필요하면 최초 비밀번호를 `.env`에 다시 설정합니다.

기존 로컬 데이터를 삭제해도 되는 경우에만 다음 명령으로 DB를 초기화합니다.

```powershell
docker compose down
docker volume rm aps-engine_postgres-data
.\scripts\run-local.ps1
```

### PostgreSQL 상태가 `healthy`가 되지 않음

컨테이너 로그에서 원인을 확인합니다.

```powershell
docker compose logs postgres
```

비밀번호 등 `.env` 값을 변경했지만 기존 PostgreSQL Volume을 계속 사용하고 있다면,
최초 컨테이너 생성 시 적용된 계정 정보와 현재 설정이 다를 수 있습니다.

### `5432` 포트가 이미 사용 중

`.env`의 `POSTGRES_PORT`를 사용 가능한 포트로 변경한 뒤 PostgreSQL을 다시 실행합니다.

```dotenv
POSTGRES_PORT=5433
```

`run-local.ps1`도 같은 `.env`를 읽으므로 Spring Boot 설정을 별도로 변경할 필요가 없습니다.

### `Port 8080 was already in use`

최신 `run-local.ps1`은 `8080`이 사용 중이면 다음 빈 포트를 자동으로 선택합니다.
터미널의 `[APS] 서버 주소:`에 출력된 실제 주소로 접속합니다.

### Gradle 다운로드 실패

첫 실행에는 `https://services.gradle.org`와 의존성 저장소에 접근할 수 있어야 합니다.
사내망 또는 방화벽 환경이라면 네트워크와 프록시 설정을 확인합니다.

## Test

일반 테스트는 다음과 같이 실행합니다. Docker를 사용할 수 있으면 PostgreSQL Testcontainers 기반
Repository 통합 테스트가 함께 실행되고, Docker를 사용할 수 없으면 해당 테스트만 자동으로 건너뜁니다.

```powershell
.\gradlew.bat test --no-daemon
```

이 명령은 GitHub Actions 빌드의 로컬 재현 명령과 같습니다.

성능 기준선은 일반 테스트와 분리해 실행합니다.

```powershell
.\gradlew.bat performanceTest --no-daemon
```

현재 기준은 600오더 학습 시나리오 5초 이내, 엔진 large 입력 5,000오더·25,000작업이며 개발 환경의
회귀 감지 기준이지 운영 SLA는 아닙니다.

컨테이너 기반 Repository 테스트만 실행하려면 다음 명령을 사용합니다.

```powershell
.\gradlew.bat test --tests "*FactoryRepositoryIntegrationTest"
```

아직 컨테이너 기반으로 전환하지 않은 심화 JPA·스케줄 테스트는 실제 로컬 PostgreSQL에 대해 다음과 같이
명시적으로 실행합니다.

```powershell
$env:APS_POSTGRES_INTEGRATION_TEST = "true"
$env:POSTGRES_DB = "aps"
$env:POSTGRES_USER = "aps"
$env:POSTGRES_PASSWORD = "<.env에 설정한 비밀번호>"
.\gradlew.bat test --tests "*PostgreSqlConnectionTest"
.\gradlew.bat test --tests "*JpaMappingTest"
.\gradlew.bat test --tests "*JpaIntegrationTest"
```

애플리케이션은 `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` 환경변수로 연결 정보를 받습니다.
시작 시 Flyway가 `src/main/resources/db/migration`의 버전 마이그레이션을 순서대로 적용하고, Hibernate는 스키마를 생성하지 않고 매핑만 검증합니다.
컨테이너 테스트 상세 정책은 [PostgreSQL Testcontainers](docs/09-testcontainers.md)를 참고해 주세요.
CI 실행 조건과 결과 artifact는 [GitHub Actions 빌드](docs/11-ci.md)를 참고해 주세요.

화면에서 생산 자원과 품목·Routing·근무시간·생산오더를 등록하고, 오더 확정 후 스케줄을 실행할 수 있습니다.
최신 실행 결과는 설비별 간트, 납기 지연과 계획기간 CAPA 사용률로 표시됩니다.
상세 설계와 현재 제외 범위는 [APS Schedule Control Tower](docs/05-mvp-ui.md)를 참고해 주세요.

---

# References

- Spring Boot
- PostgreSQL
- QueryDSL
- Docker
- Redis
- Testcontainers
- Manufacturing Execution System (MES)
- Advanced Planning & Scheduling (APS)

---

# License

MIT License
