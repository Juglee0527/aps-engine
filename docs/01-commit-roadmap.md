# APS Engine Commit-based Development Command List

이 문서는 사용자가 AI 개발 도우미에게 **한 번에 하나씩 전달할 개발 명령**을 정의합니다.

> 현행 상태: 2026-07-30 기준 `001~033`, `011-A`, `011-B` 완료 · 다음 명령 `034`

## 사용 원칙

- 아래 명령은 번호 순서대로 한 번에 하나씩 수행합니다.
- 각 명령은 하나의 논리적 커밋 범위를 갖습니다.
- AI는 현재 명령의 범위를 넘어 다음 명령을 미리 구현하지 않습니다.
- 각 단계에서 설계, 구현, 테스트, 문서 갱신까지 완료합니다.
- 각 번호의 완료 조건을 검증한 뒤 해당 번호만 체크하고 하나의 커밋을 생성합니다.
- 커밋을 현재 원격 브랜치에 push한 뒤에만 다음 번호로 넘어갑니다.
- 커밋 타입은 영어 Conventional Commit 키워드로, 콜론 뒤 제목과 상세 본문은 한국어로 작성합니다.
- 이전 단계의 테스트가 실패하면 다음 단계로 넘어가지 않습니다.
- 요구사항 변경으로 범위가 커지면 명령을 더 작은 커밋 단위로 다시 나눕니다.

---

## Phase 0. 프로젝트 기반

### [x] 001. 프로젝트 요구사항과 범위 문서화

```text
APS Engine의 목표, 핵심 용어, 구현 범위와 제외 범위를 docs/01-project.md에 정리해 주세요.
아직 애플리케이션 코드는 작성하지 마세요.
기존 README와 개발 지침을 확인하고 서로 모순되는 부분도 알려 주세요.
문서 변경은 하나의 docs 커밋 범위로 제한해 주세요.
권장 커밋 메시지: docs: define project scope and terminology
```

### [x] 002. Spring Boot 프로젝트 초기화

```text
Java 21과 Spring Boot 3.x 기반 Gradle 프로젝트를 초기화해 주세요.
기본 패키지는 프로젝트 문서에서 확정된 값을 사용하고, 확정되지 않았다면 구현 전에 질문해 주세요.
Spring Web, Validation, Spring Data JPA 의존성만 우선 추가하고 PostgreSQL, Redis, QueryDSL은 아직 설정하지 마세요.
애플리케이션 컨텍스트 로드 테스트를 작성하고 실행해 주세요.
README에 실행 방법을 반영해 주세요.
권장 커밋 메시지: chore: initialize spring boot project
```

### [x] 003. 로컬 PostgreSQL 환경 구성

```text
PostgreSQL만 실행하는 Docker Compose 구성을 추가해 주세요.
로컬 개발용 Spring 프로필과 환경변수 예시를 작성하되 비밀번호를 저장소에 직접 커밋하지 마세요.
애플리케이션의 PostgreSQL 연결 설정을 추가하고 연결 검증 방법을 docs/01-project.md에 기록해 주세요.
Redis나 비즈니스 도메인은 아직 추가하지 마세요.
권장 커밋 메시지: chore: configure local postgresql environment
```

### [x] 004. 데이터베이스 마이그레이션 기반 추가

```text
데이터베이스 스키마를 버전 관리할 수 있도록 Flyway 또는 Liquibase 중 프로젝트에 더 단순한 하나를 선택해 추가해 주세요.
선택 이유를 설명하고 빈 초기 마이그레이션 또는 기준 스키마까지만 구성해 주세요.
Hibernate ddl-auto는 운영에 안전한 값으로 설정하고 테스트로 애플리케이션 시작을 검증해 주세요.
도메인 테이블은 아직 만들지 마세요.
권장 커밋 메시지: chore: add database migration foundation
```

### [x] 005. 공통 API 오류 응답 구성

```text
Validation 오류와 예상 가능한 애플리케이션 오류를 일관되게 반환할 최소 공통 오류 응답을 구현해 주세요.
예외 계층을 과도하게 만들지 말고 ControllerAdvice, 오류 코드, 응답 record의 책임을 명확히 해 주세요.
정상 응답 래퍼는 필요성을 입증할 수 없다면 만들지 마세요.
MockMvc 테스트와 API 오류 형식 문서를 추가해 주세요.
권장 커밋 메시지: feat: add common api error handling
```

---

## Phase 1. 공장과 생산 자원

### [x] 006. Factory 도메인 모델

```text
공장을 식별하고 이름과 활성 상태를 관리하는 최소 Factory 도메인을 설계하고 구현해 주세요.
엔티티 불변조건과 생성 규칙을 도메인 메서드로 표현하고 JPA 매핑 테스트를 작성해 주세요.
아직 REST API와 생산라인은 구현하지 마세요.
ERD와 도메인 문서를 함께 갱신해 주세요.
권장 커밋 메시지: feat: add factory domain model
```

### [x] 007. Factory 등록 API

```text
Factory 등록 유스케이스와 POST API를 구현해 주세요.
요청과 응답은 record로 정의하고 입력 검증, 중복 정책, 트랜잭션 경계를 명확히 해 주세요.
서비스 및 Controller 테스트를 작성하고 API 문서를 갱신해 주세요.
조회, 수정, 삭제 기능은 구현하지 마세요.
권장 커밋 메시지: feat: add factory creation api
```

### [x] 008. Factory 조회 API

```text
Factory 단건 조회와 목록 조회 API를 구현해 주세요.
목록 데이터 증가를 고려해 페이징을 적용하고 존재하지 않는 ID의 오류 응답을 테스트해 주세요.
검색이나 복합 필터는 아직 추가하지 마세요.
API 문서를 갱신해 주세요.
권장 커밋 메시지: feat: add factory query api
```

### [x] 009. ProductionLine 도메인과 등록 API

```text
공장에 속하는 생산라인을 표현하는 ProductionLine 도메인과 등록 API를 구현해 주세요.
공장과 생산라인의 관계, 라인 코드 중복 범위, 비활성 공장에 라인을 추가할 수 있는지 정책을 명시해 주세요.
도메인, 서비스, API 테스트와 ERD/API 문서를 갱신해 주세요.
조회와 수정은 아직 구현하지 마세요.
권장 커밋 메시지: feat: add production line creation
```

### [x] 010. Machine 도메인 모델

```text
생산라인에 배치되는 Machine 도메인을 구현해 주세요.
설비 코드, 이름, 상태와 생산라인 소속만 우선 관리하고 CAPA, Calendar, Maintenance는 아직 포함하지 마세요.
설비 상태는 enum으로 명확히 표현하고 상태 변경 불변조건을 테스트해 주세요.
ERD와 도메인 문서를 갱신해 주세요.
권장 커밋 메시지: feat: add machine domain model
```

### [x] 011. Machine 등록 및 조회 API

```text
Machine 등록, 단건 조회, 생산라인별 목록 조회 API를 구현해 주세요.
존재하지 않는 생산라인, 중복 설비 코드, 잘못된 상태 입력을 검증해 주세요.
대량 목록을 고려해 페이징하고 서비스 및 API 테스트를 작성해 주세요.
CAPA 관련 기능은 구현하지 마세요.
권장 커밋 메시지: feat: add machine management api
```

### [x] 011-A. MVP용 ProductionLine 목록 조회 API

```text
Factory를 선택해 ProductionLine을 탐색할 수 있도록 Factory별 ProductionLine 목록 조회 API를 구현해 주세요.
기존 Factory와 Machine 목록의 페이징 요청·응답 중복은 공통 web 모델로 최소 정리하되 JSON 계약은 변경하지 마세요.
존재하지 않는 Factory, 잘못된 페이지 조건과 정상 목록을 테스트하고 API 문서를 갱신해 주세요.
ProductionLine 단건 조회, 수정 및 삭제는 구현하지 마세요.
권장 커밋 메시지: feat: ProductionLine 목록 조회 API 추가
```

### [x] 011-B. APS 운영 화면 MVP

```text
사용자가 현재 구현된 Factory, ProductionLine, Machine 기능을 브라우저에서 확인할 수 있는 MVP 화면을 구현해 주세요.
별도 Node 빌드 도구 없이 Spring Boot 정적 리소스로 구성하고 등록과 계층별 목록 탐색을 지원해 주세요.
데스크톱과 모바일 레이아웃, 빈 상태, API 오류 표시를 검증하고 README에 실행 방법을 갱신해 주세요.
Product, Scheduling, CAPA 화면과 인증 기능은 구현하지 마세요.
권장 커밋 메시지: feat: APS 운영 화면 MVP 추가
```

---

## Phase 2. 제품과 공정

### [x] 012. Product 도메인과 등록 API

```text
생산 대상 품목을 나타내는 Product 도메인과 등록 API를 구현해 주세요.
품목 코드, 이름, 단위, 활성 상태만 우선 포함하고 BOM과 재고는 범위에서 제외해 주세요.
품목 코드 중복과 필수값을 검증하고 테스트 및 ERD/API 문서를 작성해 주세요.
권장 커밋 메시지: feat: add product creation
```

### [x] 013. Product 조회 API

```text
Product 단건 조회와 페이징 목록 조회 API를 구현해 주세요.
품목 코드와 이름 검색이 현재 필요한지 검토하고, 필요성이 없다면 기본 조회만 구현해 주세요.
존재하지 않는 품목과 빈 목록을 테스트하고 API 문서를 갱신해 주세요.
권장 커밋 메시지: feat: add product query api
```

### [x] 014. Routing과 Operation 도메인 모델

```text
제품 생산 순서를 표현하는 Routing과 Operation 도메인을 설계하고 구현해 주세요.
Operation 순서, 표준 작업시간, 대상 설비 또는 설비 유형의 최소 관계를 명확히 정의해 주세요.
순서 중복, 음수 작업시간, 빈 공정 경로 정책을 테스트해 주세요.
REST API는 아직 구현하지 말고 도메인 문서와 ERD를 갱신해 주세요.
권장 커밋 메시지: feat: add routing and operation domain
```

### [x] 015. Routing 등록 및 조회 API

```text
제품별 Routing 등록과 조회 API를 구현해 주세요.
여러 Operation을 입력받을 때 순서와 트랜잭션 원자성을 보장하고 잘못된 제품 또는 설비 참조를 검증해 주세요.
서비스와 API 통합 테스트를 작성하고 API 문서를 갱신해 주세요.
Routing 버전 관리는 아직 구현하지 마세요.
권장 커밋 메시지: feat: add routing management api
```

---

## Phase 3. 생산오더

### [x] 016. ProductionOrder 도메인 모델

```text
ProductionOrder 도메인을 설계하고 구현해 주세요.
오더 번호, 제품, 수량, 납기일, 우선순위와 상태만 우선 포함해 주세요.
수량과 날짜의 불변조건, 허용되는 상태 전이를 도메인 메서드로 표현하고 단위 테스트를 작성해 주세요.
스케줄 결과나 자재 가용성은 아직 포함하지 마세요.
ERD와 도메인 문서를 갱신해 주세요.
권장 커밋 메시지: feat: add production order domain
```

### [x] 017. ProductionOrder 등록 API

```text
ProductionOrder 등록 유스케이스와 POST API를 구현해 주세요.
존재하지 않거나 비활성인 제품, 중복 오더 번호, 0 이하 수량, 잘못된 납기일을 검증해 주세요.
서비스 및 API 테스트를 작성하고 API 문서를 갱신해 주세요.
조회와 상태 변경은 구현하지 마세요.
권장 커밋 메시지: feat: add production order creation api
```

### [x] 018. ProductionOrder 조회 API

```text
ProductionOrder 단건 조회와 페이징 목록 조회 API를 구현해 주세요.
상태와 납기일 범위 조건만 지원하고 QueryDSL을 사용할 타당성이 있으면 이 단계에서 최소 설정해 주세요.
조건이 없는 조회, 빈 결과, 잘못된 날짜 범위를 테스트해 주세요.
API 문서를 갱신해 주세요.
권장 커밋 메시지: feat: add production order query api
```

### [x] 019. ProductionOrder 상태 변경

```text
ProductionOrder의 확정, 착수, 완료, 취소 상태 변경 유스케이스를 구현해 주세요.
허용 상태 전이표를 먼저 문서화하고 도메인 계층에서 잘못된 전이를 차단해 주세요.
각 정상 전이와 금지 전이를 단위 테스트 및 API 테스트로 검증해 주세요.
스케줄링 실행은 아직 연결하지 마세요.
권장 커밋 메시지: feat: add production order status transitions
```

---

## Phase 4. CAPA

### [x] 020. WorkingCalendar 도메인 모델

```text
설비의 가동 가능 시간을 표현하는 WorkingCalendar 도메인을 설계하고 구현해 주세요.
근무일, 시작·종료 시각, 휴무만 우선 지원하고 교대조와 반복 규칙은 제외해 주세요.
시간대, 자정 경계, 겹치는 시간 구간 정책을 명시하고 단위 테스트를 작성해 주세요.
ERD와 CAPA 문서를 갱신해 주세요.
권장 커밋 메시지: feat: add working calendar domain
```

### [x] 021. 설비 가용시간 조회

```text
설비와 날짜 범위를 입력받아 실제 가용시간 구간을 조회하는 서비스를 구현해 주세요.
휴무, 일부 가동, 빈 캘린더, 잘못된 날짜 범위를 테스트해 주세요.
아직 생산오더 부하는 차감하지 마세요.
필요한 최소 API와 CAPA 계산 규칙 문서를 추가해 주세요.
권장 커밋 메시지: feat: calculate machine available time
```

### [x] 022. 설비 CAPA 계산

```text
설비의 가용시간과 계획 부하를 기준으로 기간별 CAPA 사용률을 계산하는 서비스를 구현해 주세요.
사용 가능 시간, 할당 시간, 잔여 시간, 사용률의 계산식을 명시하고 0으로 나누는 경우를 처리해 주세요.
정상, CAPA 초과, 가용시간 0, 기간 경계 테스트를 작성해 주세요.
스케줄 자동 할당은 아직 구현하지 마세요.
권장 커밋 메시지: feat: calculate machine capacity utilization
```

---

## Phase 5. 기본 스케줄링

### [x] 023. Scheduling 입력과 결과 모델

```text
스케줄링 실행에 필요한 입력 모델과 결과 모델을 설계해 주세요.
ProductionOrder, Operation, Machine, 시간 구간 사이의 데이터 흐름과 책임을 문서화하고 순수 Java 모델로 구현해 주세요.
알고리즘, DB 저장, REST API는 아직 구현하지 마세요.
유효하지 않은 입력 모델의 단위 테스트를 작성해 주세요.
권장 커밋 메시지: feat: add scheduling input and result model
```

### [x] 024. 단일 설비 순방향 스케줄링

```text
하나의 설비와 하나의 생산오더를 대상으로 가장 단순한 순방향 스케줄링 알고리즘을 구현해 주세요.
설비 가용시간 안에서 Operation을 순서대로 배치하고 작업시간이 가용구간을 넘는 경우를 명확히 처리해 주세요.
알고리즘은 DB나 Spring에 의존하지 않는 순수 Java 코드로 작성하고 단위 테스트를 충분히 추가해 주세요.
다중 설비와 최적화는 구현하지 마세요.
권장 커밋 메시지: feat: add single machine forward scheduler
```

### [x] 025. 다중 Operation 스케줄링

```text
하나의 생산오더에 포함된 여러 Operation을 선후관계에 맞게 배치하도록 순방향 스케줄러를 확장해 주세요.
이전 Operation 종료 전 다음 Operation이 시작되지 않도록 보장하고 서로 다른 설비의 캘린더를 반영해 주세요.
공정 간 대기시간과 설비 가용구간 경계 테스트를 작성해 주세요.
여러 생산오더의 경쟁은 아직 처리하지 마세요.
권장 커밋 메시지: feat: schedule sequential operations
```

### [x] 026. 다중 생산오더 우선순위 규칙

```text
여러 ProductionOrder를 스케줄링할 때 적용할 첫 Priority Rule을 구현해 주세요.
납기일 우선 또는 명시적 우선순위 중 하나를 선택하고 선택 근거와 동률 처리 규칙을 문서화해 주세요.
정렬 정책은 교체 가능한 최소 인터페이스로 분리하되 불필요한 전략 계층은 만들지 마세요.
정상, 동률, 빈 입력 테스트를 작성해 주세요.
권장 커밋 메시지: feat: add production order priority rule
```

### [x] 027. 스케줄 실행 유스케이스

```text
확정된 ProductionOrder를 조회해 스케줄러를 실행하고 결과를 반환하는 애플리케이션 유스케이스를 구현해 주세요.
도메인 알고리즘과 JPA 조회 책임을 분리하고 트랜잭션 경계를 명확히 해 주세요.
아직 스케줄 결과를 DB에 저장하거나 확정하지 마세요.
서비스 통합 테스트와 실행 흐름 문서를 작성해 주세요.
권장 커밋 메시지: feat: add scheduling execution use case
```

### [x] 028. 스케줄 결과 저장

```text
검토된 스케줄 결과를 DB에 저장하는 모델과 유스케이스를 구현해 주세요.
동일 실행의 중복 저장, 부분 저장 실패, 원본 생산오더 변경 시 처리 정책을 먼저 정의해 주세요.
저장과 조회 통합 테스트를 작성하고 ERD 및 스케줄링 문서를 갱신해 주세요.
스케줄 확정과 재스케줄링은 아직 구현하지 마세요.
권장 커밋 메시지: feat: persist scheduling results
```

---

## Phase 6. 제조 제약조건

### [x] 029. Changeover Time 모델

```text
제품 전환에 따른 Changeover Time을 표현하는 도메인 모델을 설계하고 구현해 주세요.
설비, 이전 제품, 다음 제품 조합별 전환시간과 기본값 정책을 명시해 주세요.
동일 제품, 매핑 없음, 음수 시간, 방향성이 다른 조합을 테스트해 주세요.
스케줄러에는 아직 적용하지 마세요.
권장 커밋 메시지: feat: add changeover time domain
```

### [x] 030. 스케줄러에 Changeover Time 적용

```text
기존 스케줄러가 연속 작업 사이의 Changeover Time을 반영하도록 확장해 주세요.
기존 단일·다중 Operation 동작을 깨지 않도록 회귀 테스트를 유지하고 캘린더 경계를 넘는 전환시간을 테스트해 주세요.
Changeover 데이터를 찾는 책임과 스케줄 계산 책임을 분리해 주세요.
권장 커밋 메시지: feat: apply changeover time to scheduler
```

### [x] 031. Maintenance 제약조건

```text
설비의 계획 정비 시간대를 가용시간에서 제외하는 Maintenance 제약조건을 구현해 주세요.
정비 구간 중복, 캘린더 밖 정비, 작업과 정비 경계가 맞닿는 경우를 정의하고 테스트해 주세요.
기존 WorkingCalendar와 역할이 중복되지 않도록 설계 이유를 문서화해 주세요.
권장 커밋 메시지: feat: add machine maintenance constraint
```

### [x] 032. Lead Time 계산

```text
저장된 스케줄 결과를 기준으로 생산오더의 계획 Lead Time을 계산하는 기능을 구현해 주세요.
가공시간, 대기시간, Changeover Time을 구분해 반환하고 계산 기준을 문서화해 주세요.
Operation 없음, 하루 경계, 휴무 포함 케이스를 테스트해 주세요.
통계나 예측 기능은 추가하지 마세요.
권장 커밋 메시지: feat: calculate planned lead time
```

### [x] 033. Bottleneck 탐지

```text
기간별 설비 CAPA 사용률을 기준으로 Bottleneck 후보를 탐지하는 첫 번째 규칙을 구현해 주세요.
임계값과 정렬 기준을 명시하고, 이것이 최적화 알고리즘이 아닌 진단 규칙임을 문서화해 주세요.
가용시간 0, 동일 사용률, CAPA 초과 설비를 테스트해 주세요.
자동 재배치는 구현하지 마세요.
권장 커밋 메시지: feat: detect capacity bottlenecks
```

---

## Phase 7. 운영 기반

### [ ] 034. Redis 캐시 적용 대상 검증

```text
현재 구현에서 Redis 캐시가 실제로 필요한 조회 경로를 측정 또는 구조적 근거로 선정해 주세요.
선정된 읽기 전용 조회 하나에만 캐시를 적용하고 TTL, 캐시 키, 무효화 정책을 문서화해 주세요.
캐시 장애 시 원본 기능이 동작하는지 테스트해 주세요.
근거가 부족하면 코드를 추가하지 말고 분석 문서만 작성해 주세요.
권장 커밋 메시지: perf: add cache for verified query path
```

### [ ] 035. Testcontainers 통합 테스트 기반

```text
PostgreSQL Testcontainers를 사용하는 공통 통합 테스트 기반을 구성해 주세요.
공통 설정은 테스트 중복을 줄이는 최소 범위로만 만들고 실제 Repository 통합 테스트 하나로 동작을 검증해 주세요.
Docker를 사용할 수 없는 환경에서의 실행 조건도 문서화해 주세요.
Redis 컨테이너는 아직 추가하지 마세요.
권장 커밋 메시지: test: add postgresql testcontainers support
```

### [ ] 036. Docker 애플리케이션 이미지

```text
APS Engine 애플리케이션의 재현 가능한 Docker 이미지를 구성해 주세요.
멀티 스테이지 빌드, 비루트 실행, 환경변수 주입, 헬스체크를 검토하고 불필요한 설정은 제외해 주세요.
PostgreSQL과 함께 실행하는 Docker Compose 구성을 검증하고 실행 방법을 README에 작성해 주세요.
권장 커밋 메시지: chore: add application docker image
```

### [ ] 037. GitHub Actions 빌드 검증

```text
push와 pull request에서 Java 21 Gradle 테스트를 실행하는 최소 GitHub Actions 워크플로를 추가해 주세요.
Gradle 캐시, Wrapper 검증, 테스트 결과 보존을 검토하고 현재 프로젝트에 필요한 것만 적용해 주세요.
배포 작업은 추가하지 마세요.
CI 실행 조건과 로컬 재현 명령을 README에 작성해 주세요.
권장 커밋 메시지: ci: add gradle build workflow
```

### [ ] 038. 스케줄링 성능 기준선

```text
현재 스케줄링 알고리즘의 성능 기준선을 측정할 수 있는 테스트를 작성해 주세요.
소규모, 중간 규모, 대규모 입력 크기를 명시하고 실행시간과 메모리 관찰 방법을 docs/07-performance.md에 기록해 주세요.
측정 결과 없이 최적화 코드를 먼저 추가하지 마세요.
일반 단위 테스트와 성능 테스트의 실행 태스크를 분리해 주세요.
권장 커밋 메시지: test: add scheduling performance baseline
```

### [ ] 039. 측정 기반 성능 개선

```text
docs/07-performance.md의 기준선 결과에서 확인된 가장 큰 병목 하나만 개선해 주세요.
변경 전후 측정값, 시간·공간 복잡도, 동작 동일성을 설명하고 회귀 테스트를 작성해 주세요.
측정으로 효과를 입증할 수 없는 최적화는 적용하지 마세요.
권장 커밋 메시지: perf: optimize verified scheduling bottleneck
```

---

## Phase 8. APS 엔진 고도화

### [ ] 040. Operation 대체 설비 모델

```text
Operation이 하나 이상의 후보 설비를 가질 수 있도록 대체 설비 모델을 설계하고 구현해 주세요.
기존 고정 설비 계약과의 호환성, 후보 우선순위, 비가용·비활성 설비 제외 정책을 명시해 주세요.
후보 없음, 중복 후보, 우선순위 동률과 기존 단일 설비 Routing을 테스트해 주세요.
스케줄러의 설비 선택은 아직 변경하지 마세요.
권장 커밋 메시지: feat: add alternative machine model
```

### [ ] 041. 결정론적 대체 설비 선택

```text
각 Operation의 후보 설비 중 가장 이른 완료시각을 만드는 설비를 선택하도록 스케줄러를 확장해 주세요.
완료시각이 같으면 후보 우선순위와 설비 ID로 동률을 처리해 같은 입력이 같은 결과를 만들게 해 주세요.
설비별 캘린더, 기존 부하, 선행 공정 종료와 Changeover Time을 함께 반영하고 회귀 테스트를 유지해 주세요.
전역 최적화나 상용 Solver는 추가하지 마세요.
권장 커밋 메시지: feat: select earliest alternative machine
```

### [ ] 042. Dispatching Rule과 계획 KPI 비교

```text
명시적 우선순위, EDD, SPT 규칙을 선택해 같은 입력을 실행할 수 있도록 Priority Rule 구성을 확장해 주세요.
ScheduleRun에 적용 규칙을 기록하고 총 지연시간, 납기 지연 오더 수, Makespan, 설비 가동률 KPI를 계산해 주세요.
동률 처리와 KPI 계산식을 문서화하고 같은 입력의 규칙별 결과 차이를 테스트해 주세요.
규칙을 자동 선택하거나 AI 추천하는 기능은 추가하지 마세요.
권장 커밋 메시지: feat: compare dispatching rules and schedule kpis
```

### [ ] 043. Frozen Horizon 재스케줄링

```text
기존 ScheduleRun과 동결 기준시각을 입력받아 기준시각 이전 작업은 유지하고 이후 작업만 다시 배치하는 재스케줄링을 구현해 주세요.
진행 중인 작업, 동결 경계와 겹치는 작업, 취소되거나 새로 추가된 오더 정책을 먼저 문서화해 주세요.
원본 실행은 변경하지 않고 새 ScheduleRun으로 결과를 저장하며 추적 관계를 남겨 주세요.
수동 Drag & Drop과 실시간 MES 이벤트 연동은 추가하지 마세요.
권장 커밋 메시지: feat: reschedule with frozen horizon
```

---

## Phase 9. 데이터 처리와 실행 운영

### [ ] 044. CSV 대량 입력 검증과 미리보기

```text
Factory, ProductionLine, Machine, Product, Routing, ProductionOrder 데이터를 CSV 파일로 읽어 검증하는 유스케이스를 구현해 주세요.
파일 크기와 행 수 제한, 필수 열, 코드 정규화, 참조 순서와 행 단위 오류 형식을 명시해 주세요.
DB에는 반영하지 않고 성공·실패 예상 건수와 행별 오류를 미리보기로 반환해 검증과 반영 책임을 분리해 주세요.
Excel 전용 서식과 범용 ETL 프레임워크는 추가하지 마세요.
권장 커밋 메시지: feat: preview planning data csv import
```

### [ ] 045. 대량 입력 멱등성과 실패 복구

```text
검증을 통과한 CSV 대량 입력을 DB에 반영하고 요청 고유 키로 같은 파일의 재요청이 중복 데이터를 만들지 않도록 구현해 주세요.
입력 실행 상태, 성공·실패 행, 오류 사유를 저장하고 중단된 실행의 재시도 정책을 명시해 주세요.
중복 요청, 일부 참조 오류, DB 제약 위반과 대량 데이터 경계를 통합 테스트로 검증해 주세요.
메시지 브로커는 아직 추가하지 마세요.
권장 커밋 메시지: feat: make bulk import idempotent
```

### [ ] 046. 비동기 스케줄 실행과 이력 조회

```text
스케줄 실행 요청과 계산을 분리해 요청은 실행 ID를 반환하고 상태를 조회할 수 있도록 구현해 주세요.
QUEUED, RUNNING, COMPLETED, FAILED 상태와 실패 사유, 중복 executionKey, 동시 실행 정책을 명시해 주세요.
애플리케이션 재시작 시 RUNNING 작업 처리와 트랜잭션 경계를 테스트해 주세요.
외부 메시지 브로커와 분산 실행은 추가하지 마세요.
권장 커밋 메시지: feat: execute schedules asynchronously
```

### [ ] 047. 스케줄 실행 관측성

```text
스케줄 실행시간, 입력 오더·공정 수, 생성 작업 수, 실패 수와 DB 쿼리 관찰 방법을 추가해 주세요.
Spring Boot Actuator와 Micrometer의 최소 기능을 사용하고 민감한 입력 데이터는 메트릭이나 로그에 남기지 마세요.
성공·실패 실행의 구조화 로그와 핵심 메트릭을 테스트하고 운영 확인 절차를 문서화해 주세요.
외부 APM, 로그 수집 플랫폼과 클라우드 모니터링 구성은 추가하지 마세요.
권장 커밋 메시지: feat: add scheduling observability
```

---

## 현재 요청 템플릿

새로운 요구사항이 생겨 위 목록에 없는 작업을 요청할 때는 다음 형식을 사용합니다.

```text
[작업명]을 하나의 커밋 범위로 구현해 주세요.

포함 범위:
- 

제외 범위:
- 

완료 조건:
- 설계 이유 설명
- 실행 가능한 구현
- 정상 및 엣지 케이스 테스트
- 관련 docs와 README 갱신

다음 기능은 미리 구현하지 마세요.
커밋은 생성하지 말고 권장 Conventional Commit 메시지만 제안해 주세요.
```
