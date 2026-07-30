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

2026년 7월 30일 기준으로 Factory부터 병목 설비 진단까지 로드맵 `001~033`을 구현했습니다.

```text
Factory → ProductionLine → Machine → WorkingCalendar
Product → Routing → Operation
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
- 다음 개발 단위: `034. Redis 캐시 적용 대상 검증`

# Tech Stack

## Applied

| 구분 | 현재 적용 기술 |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.16, Gradle 8.14.4 |
| Persistence | Spring Data JPA, Hibernate, Flyway |
| Database | PostgreSQL 18.4 |
| Test | JUnit 5, Mockito, Spring Boot Test |
| Local Environment | Docker Compose |
| UI | Spring Boot Static Resources, Vanilla JavaScript |

## Planned

QueryDSL, Redis, Testcontainers, 애플리케이션 Docker 이미지와 GitHub Actions는 목표 스택이며 아직 적용하지 않았습니다.
각 기술은 로드맵 `034~037`에서 필요성을 검증한 뒤 추가합니다.

---

# Roadmap

개발은 `001~047`의 핵심 단위와 `011-A`, `011-B` 보조 MVP 단위로 진행합니다.

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

상세 범위와 현재 진행 상태는 [커밋 단위 개발 로드맵](docs/01-commit-roadmap.md)에서 관리합니다.

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

🚧 In Progress

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
- [ ] 034. Redis 캐시 적용 대상 검증
- [ ] 035. Testcontainers 통합 테스트 기반
- [ ] 036. Docker 애플리케이션 이미지
- [ ] 037. GitHub Actions 빌드 검증
- [ ] 038. 스케줄링 성능 기준선
- [ ] 039. 측정 기반 성능 개선
- [ ] 040. Operation 대체 설비 모델
- [ ] 041. 결정론적 대체 설비 선택
- [ ] 042. Dispatching Rule과 계획 KPI 비교
- [ ] 043. Frozen Horizon 재스케줄링
- [ ] 044. CSV 대량 입력 검증과 미리보기
- [ ] 045. 대량 입력 멱등성과 실패 복구
- [ ] 046. 비동기 스케줄 실행과 이력 조회
- [ ] 047. 스케줄 실행 관측성

상세 진행 상태는 [커밋 단위 개발 로드맵](docs/01-commit-roadmap.md)을 참고해 주세요.

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

처음 사용하는 경우 화면의 `사용자 가이드` 메뉴에서 샘플 데이터를 단계별로 등록하며
다음 흐름을 바로 확인할 수 있습니다. 각 단계는 완료 후 다음 단계가 활성화되며,
중간에 실패해도 이미 등록된 샘플 데이터부터 이어서 진행합니다.

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

일반 단위 테스트는 다음과 같이 실행합니다.

```powershell
.\gradlew.bat test
```

실제 로컬 PostgreSQL 연결 및 JPA 매핑 테스트는 다음과 같이 명시적으로 실행합니다.

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
