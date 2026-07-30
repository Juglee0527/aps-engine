# PostgreSQL Testcontainers

## 1. 목적

Repository 통합 테스트가 개발자 PC의 고정 포트, 데이터베이스 이름과 사전 데이터에 의존하지 않도록
PostgreSQL Testcontainers 공통 기반을 제공합니다.

```text
JUnit 5
  → PostgreSQL 17 container
    → 동적 JDBC URL·계정 주입
      → Flyway 전체 마이그레이션
        → Hibernate schema validation
          → Repository 통합 테스트
```

## 2. 공통 기반

`PostgreSqlContainerIntegrationTest`는 다음 책임만 가집니다.

- `postgres:17-alpine` 컨테이너 수명 관리
- 임의 호스트 포트의 JDBC URL, 사용자와 비밀번호 주입
- Hibernate `ddl-auto=validate`
- Flyway migration 파일명 검증
- Docker를 사용할 수 없을 때 컨테이너 테스트 자동 skip
- 테스트 클래스 종료 후 Spring Context를 폐기해 다음 컨테이너의 동적 JDBC URL과 연결 풀이 섞이지 않게 격리

테스트용 스키마 SQL을 별도로 복제하지 않습니다. 애플리케이션과 같은
`src/main/resources/db/migration`을 적용해 운영 스키마 계약과의 차이를 막습니다.

## 3. 적용 대상

`FactoryRepositoryIntegrationTest`가 공통 기반을 상속하고 실제 `FactoryRepository`로 다음 흐름을
검증합니다.

```text
Factory 생성
  → saveAndFlush
    → 영속성 컨텍스트 초기화
      → findById
        → 필드와 활성 상태 검증
```

테스트는 트랜잭션 rollback을 사용하므로 다른 테스트에 데이터를 남기지 않습니다.

`PlanningDataImportExecutionPostgreSqlIntegrationTest`는 실제 PostgreSQL 제약과 트랜잭션으로
다음 경계를 추가 검증합니다.

- 완전한 CSV 반영과 동일 요청의 멱등 응답
- 동일 요청 키·다른 파일 충돌
- 일부 참조 오류 시 전체 미반영
- DB 유일 제약 위반 시 전체 롤백과 실패 이력
- 중단 실행의 같은 파일 재시도
- 허용 상한 2,000행 반영

이 테스트는 클래스 전용 컨테이너에서 각 테스트 전에 관련 테이블을 초기화해
`REQUIRES_NEW`로 커밋되는 실행 이력까지 격리합니다.

`ScheduleExecutionPostgreSqlIntegrationTest`는 실행 키 멱등성·요청 충돌, 계획 offset 보존,
재스케줄 요청, `RUNNING`과 이미 커밋된 ScheduleRun의 재시작 대조, 결과 없는 중단 실행의
`FAILED` 전이를 검증합니다.

`SchedulingObservabilityPostgreSqlIntegrationTest`는 Hibernate 통계를 명시적으로 활성화한
별도 컨텍스트에서 실제 Repository 쿼리를 실행하고 `hibernate.query.executions` Micrometer
카운터가 증가하는지 검증합니다. 기본 애플리케이션 설정에서는 통계 수집을 비활성화합니다.

## 4. 실행

Docker Desktop 또는 호환 Docker daemon이 실행 중이면 일반 테스트 명령에 컨테이너 테스트가
자동 포함됩니다.

```powershell
.\gradlew.bat test
```

해당 테스트만 실행할 수도 있습니다.

```powershell
.\gradlew.bat test --tests "*FactoryRepositoryIntegrationTest"
.\gradlew.bat test --tests "*PlanningDataImportExecutionPostgreSqlIntegrationTest"
.\gradlew.bat test --tests "*ScheduleExecutionPostgreSqlIntegrationTest"
.\gradlew.bat test --tests "*SchedulingObservabilityPostgreSqlIntegrationTest"
```

첫 실행은 `postgres:17-alpine` 이미지를 내려받으므로 네트워크 상태에 따라 시간이 더 걸릴 수 있습니다.

## 5. Docker를 사용할 수 없는 환경

`@Testcontainers(disabledWithoutDocker = true)` 정책으로 컨테이너 기반 클래스만 skip됩니다.
순수 단위 테스트와 Controller·Service 테스트는 계속 실행됩니다. Docker가 없는 것을 전체 테스트 실패로
간주하지 않지만, CI에서는 Docker 사용이 가능해야 실제 Repository 검증 근거가 남습니다.

기존 `APS_POSTGRES_INTEGRATION_TEST=true` 기반 심화 JPA·스케줄 테스트는 아직 로컬 PostgreSQL을
명시적으로 사용하는 별도 검증 경로입니다. 한 번에 모두 전환하지 않고 이후 필요한 통합 테스트부터
공통 기반을 사용합니다.

## 6. 제외 범위

- Redis 컨테이너
- 컨테이너 재사용과 전역 singleton 최적화
- 테스트 병렬 실행
- 애플리케이션 실행용 Docker Compose

애플리케이션 이미지와 Compose는 로드맵 `036`에서 별도로 다룹니다.
