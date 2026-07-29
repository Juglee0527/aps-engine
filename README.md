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

# Tech Stack

## Backend

- Java 21
- Spring Boot 3
- Spring Data JPA
- QueryDSL
- Hibernate

## Database

- PostgreSQL
- Redis

## Test

- JUnit 5
- Testcontainers
- Mockito

## DevOps

- Docker
- GitHub Actions

---

# Roadmap

개발은 검증 가능한 39개 커밋 단위로 진행합니다.

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
├── docker
├── src
│   ├── main
│   └── test
├── .github
│   └── workflows
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
- [ ] 007. Factory 등록 API

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

실제 로컬 PostgreSQL 연결 테스트는 다음과 같이 명시적으로 실행합니다.

```powershell
$env:APS_POSTGRES_INTEGRATION_TEST = "true"
$env:POSTGRES_DB = "aps"
$env:POSTGRES_USER = "aps"
$env:POSTGRES_PASSWORD = "<.env에 설정한 비밀번호>"
.\gradlew.bat test --tests "*PostgreSqlConnectionTest"
```

## Run

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local"
```

애플리케이션은 `POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` 환경변수로 연결 정보를 받습니다.
시작 시 Flyway가 `src/main/resources/db/migration`의 버전 마이그레이션을 순서대로 적용하고, Hibernate는 스키마를 생성하지 않고 매핑만 검증합니다.

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
