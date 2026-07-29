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

현재 APS Engine의 핵심 기능을 단계적으로 구현 중입니다.

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
