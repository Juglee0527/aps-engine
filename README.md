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

## Phase 1

- [ ] 프로젝트 초기 구성
- [ ] Docker 환경 구성
- [ ] PostgreSQL 연동
- [ ] Redis 연동
- [ ] GitHub Actions

---

## Phase 2

생산 마스터 관리

- [ ] Factory
- [ ] Production Line
- [ ] Machine
- [ ] Product
- [ ] Work Center

---

## Phase 3

주문 관리

- [ ] Sales Order
- [ ] Production Order
- [ ] Priority
- [ ] Due Date

---

## Phase 4

생산계획

- [ ] Capacity Planning
- [ ] Machine Assignment
- [ ] Production Scheduling
- [ ] Lead Time
- [ ] Calendar

---

## Phase 5

Constraint

- [ ] Machine Capacity
- [ ] Working Calendar
- [ ] Maintenance
- [ ] Changeover Time
- [ ] Material Availability

---

## Phase 6

Optimization

- [ ] Scheduling Algorithm
- [ ] Priority Rule
- [ ] Load Balancing
- [ ] Bottleneck Detection

---

## Phase 7

Performance

- [ ] Query Optimization
- [ ] Index Optimization
- [ ] Batch Processing
- [ ] Redis Cache

---

## Phase 8

Testing

- [ ] Unit Test
- [ ] Integration Test
- [ ] API Test
- [ ] Performance Test

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

- Architecture
- ERD
- API Specification
- Scheduler Design
- Capacity Planning
- Constraint Design
- Performance Optimization
- Test Strategy

---

# Development Principles

- Domain-Driven Design(DDD)
- Clean Architecture
- Testable Code
- Object-Oriented Programming
- SOLID Principles
- Continuous Refactoring

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
