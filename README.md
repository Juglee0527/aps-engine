# APS Engine

> Production Scheduling & Planning Engine built with Spring Boot

APS Engine은 제조업의 생산계획(APS, Advanced Planning & Scheduling)의 핵심 기능을 직접 구현하며 학습하는 프로젝트입니다.

단순 CRUD 프로젝트가 아니라 실제 제조 현장에서 사용하는 APS의 생산계획, 설비(Capacity), 작업 스케줄링, 제약조건(Constraint), 최적화 로직을 단계적으로 구현하는 것을 목표로 합니다.

---

# Goals

- APS 도메인 이해
- 생산계획(Scheduling) 알고리즘 구현
- 설비 CAPA(Capacity Planning) 계산
- 생산 제약조건(Constraint) 처리
- 성능 최적화
- 테스트 코드 및 CI/CD 구축
- 실무 수준의 Spring Boot 프로젝트 설계

---

# Current Implementation

2026년 7월 30일 기준으로 Factory부터 스케줄 결과 저장까지 로드맵 `001~028`을 구현했습니다.

```text
Factory → ProductionLine → Machine → WorkingCalendar
Product → Routing → Operation
ProductionOrder → ForwardScheduler → ScheduleRun → ScheduledOperation
```

- 명시적 우선순위, 납기와 식별자를 적용하는 순방향 스케줄러
- 설비 근무시간과 공정 선후관계를 반영한 유한 CAPA 배정
- 실행 키 기반 중복 방지와 스케줄 결과 원자적 저장
- 실제 API 데이터 기반 설비 간트, 납기 지연 및 병목 후보 화면
- 다음 개발 단위: `029. Changeover Time 모델`

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

개발은 `001~039`의 핵심 단위와 `011-A`, `011-B` 보조 MVP 단위로 진행합니다.

- Phase 0: 프로젝트 기반
- Phase 1: 공장과 생산 자원
- Phase 2: 제품과 공정
- Phase 3: 생산오더
- Phase 4: CAPA
- Phase 5: 기본 스케줄링
- Phase 6: 제조 제약조건
- Phase 7: 운영 기반

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
- [ ] 029. Changeover Time 모델

상세 진행 상태는 [커밋 단위 개발 로드맵](docs/01-commit-roadmap.md)을 참고해 주세요.

---

# Local Development

## Requirements

- Java 21
- Docker Desktop

별도의 Gradle 설치는 필요하지 않습니다. 저장소에 포함된 Gradle Wrapper를 사용합니다.

## PostgreSQL

환경변수 예시 파일을 복사한 뒤 로컬 개발용 비밀번호를 변경합니다.

```powershell
Copy-Item .env.example .env
docker compose up -d postgres
docker compose ps
```

`.env`는 Git 추적 대상에서 제외됩니다. 실제 비밀번호를 `.env.example`이나 소스 코드에 기록하지 마세요.
호스트의 `5432` 포트가 이미 사용 중이면 `.env`의 `POSTGRES_PORT`를 빈 포트로 변경합니다.

## Test

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

## Run

```powershell
.\scripts\run-local.ps1
```

애플리케이션은 `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` 환경변수로 연결 정보를 받습니다.
`run-local.ps1`은 Git에서 제외된 `.env`를 현재 프로세스에만 읽어들인 뒤 로컬 프로필로 서버를 실행합니다.
시작 시 Flyway가 `src/main/resources/db/migration`의 버전 마이그레이션을 순서대로 적용하고, Hibernate는 스키마를 생성하지 않고 매핑만 검증합니다.

서버가 시작되면 브라우저에서 `http://localhost:8080`에 접속해 APS Schedule Control Tower를 확인할 수 있습니다.
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
