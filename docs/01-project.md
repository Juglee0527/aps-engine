# APS Engine 프로젝트 정의

## 1. 프로젝트 목적

`aps-engine`은 제조 현장의 생산계획 문제를 이해하고 해결하기 위해 APS(Advanced Planning & Scheduling)의 핵심 기능을 단계적으로 구현하는 오픈소스 학습 프로젝트입니다.

단순 CRUD 예제가 아니라 다음 질문에 답할 수 있는 실행 가능한 시스템을 목표로 합니다.

- 어떤 생산오더를 언제 시작해야 하는가?
- 어떤 설비와 생산라인에 작업을 배정해야 하는가?
- 주어진 가용시간 안에서 납기와 우선순위를 어떻게 반영할 것인가?
- CAPA 부족과 병목 설비를 어떻게 발견할 것인가?
- 정비, 휴무, Changeover Time 같은 제조 제약을 일정에 어떻게 반영할 것인가?

코드 품질뿐 아니라 설계 근거, 테스트 전략, 성능 변화도 문서로 남겨 면접 포트폴리오로 설명할 수 있는 수준을 지향합니다.

## 2. 기술 기준

| 구분 | 기술 |
| --- | --- |
| Language | Java 21 |
| Framework | Spring Boot 3.5.16 |
| Build | Gradle 8.14.4 Wrapper |
| Database | PostgreSQL |
| ORM | Spring Data JPA, QueryDSL |
| Cache | Redis |
| Test | JUnit 5, Mockito, Testcontainers |
| Container | Docker, Docker Compose |
| CI | GitHub Actions |

기본 Java 패키지는 저장소 소유자와 프로젝트명을 기준으로 다음 값을 사용합니다.

```text
com.github.juglee0527.apsengine
```

Spring Boot 3.x 요구사항을 유지하면서 2026년 7월 기준 마지막 공개 3.x 릴리스인 3.5.16을 사용합니다. Java 21은 이 버전의 공식 지원 범위에 포함됩니다.

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

## 5. 현재 제외 범위

다음 기능은 현재 39개 커밋 로드맵에 포함하지 않습니다.

- 수요예측과 판매계획
- MRP와 원자재 재고 가용성
- BOM 및 대체 자재
- 구매오더와 공급업체 관리
- 인력과 작업자 스킬 기반 배정
- 운송 및 물류 최적화
- 다공장 간 물량 배분
- 실시간 MES 설비 데이터 수집
- 스케줄 결과의 시각화 UI
- 상용 Solver 또는 AI 기반 최적화
- 멀티테넌시, 사용자 관리 및 인증·인가
- 운영 배포와 클라우드 인프라

제외 기능은 명시적인 로드맵 변경 없이 미리 구현하지 않습니다.

## 6. 아키텍처 경계

Layered Architecture를 기본으로 사용하며 기능별 패키지 안에서 다음 책임을 구분합니다.

```text
API 요청
  → Controller: HTTP 입력 검증과 응답 변환
  → Application Service: 유스케이스와 트랜잭션 경계
  → Domain: 상태, 규칙, 불변조건
  → Repository/Infrastructure: 영속화와 외부 기술 연동
```

Clean Architecture와 DDD의 개념은 도메인 규칙을 보호하는 데 필요한 만큼만 적용합니다. 현재 요구사항에 필요하지 않은 Port, Adapter, 추상 Repository 또는 범용 프레임워크는 만들지 않습니다.

## 7. 개발 완료 기준

각 커밋 단위는 다음 조건을 모두 만족해야 완료로 판단합니다.

- 요청된 범위만 구현되어 있습니다.
- 설계 선택과 제외 범위가 설명되어 있습니다.
- 실행 가능한 코드와 관련 테스트가 있습니다.
- 정상, 경계값 및 오류 케이스가 검토되었습니다.
- 관련 문서와 README가 실제 구현 상태를 반영합니다.
- Gradle 테스트가 통과하거나 환경상 미실행 사유가 기록되어 있습니다.
- 하나의 Conventional Commit으로 커밋되어 원격 브랜치에 push되었습니다.
- 로드맵 체크박스가 완료 상태로 변경되었습니다.

## 8. 기존 문서 정합성 검토

001 단계에서 확인한 문서 차이는 다음과 같이 정리합니다.

| 항목 | 기존 README | 개발 지침 및 확정 방향 |
| --- | --- | --- |
| 단계 번호 | README는 프로젝트 기반을 Phase 1로 시작 | 커밋 로드맵은 프로젝트 기반을 Phase 0으로 정의 |
| 아키텍처 | DDD와 Clean Architecture를 원칙으로 단순 표기 | Layered Architecture를 기본으로 하고 필요한 개념만 제한적으로 적용 |
| Redis 시점 | 초기 구성과 성능 단계에 모두 표시 | 실제 필요성이 확인되는 034 단계에서 적용 여부 결정 |
| 범위 | Work Center, Sales Order, Material Availability, Load Balancing 포함 | 현재 39개 커밋 로드맵에서는 제외 |
| 진행상태 | 포괄적인 `In Progress`만 표시 | 번호별 체크박스와 원격 push 여부로 추적 |

이 문서와 커밋 단위 로드맵을 상세 범위의 기준으로 사용합니다. README는 프로젝트 소개와 현재 상태를 간결하게 보여주는 진입점으로 유지합니다.
