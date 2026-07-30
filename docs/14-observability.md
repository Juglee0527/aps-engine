# 스케줄 실행 관측성

## 1. 목적과 범위

외부 APM이나 로그 수집 플랫폼 없이 Spring Boot Actuator와 Micrometer의 최소 기능으로
비동기 스케줄 실행 상태를 확인합니다.

```text
ScheduleExecutionWorker
  → 실행 처리시간
  → 완료 결과의 오더·공정·작업 수
  → 실패 단계
    → Micrometer registry
      → GET /actuator/metrics
```

메트릭은 프로세스 시작 이후 누적값입니다. 장기 보관, 대시보드, 경보와 여러 인스턴스의
집계는 현재 범위가 아닙니다.

## 2. 핵심 메트릭

| 메트릭 | 형식 | 태그 | 의미 |
| --- | --- | --- | --- |
| `aps.schedule.execution.duration` | Timer | `outcome=success\|failure` | 작업자가 실행 상태 전이를 시작한 시점부터 완료·실패 처리까지 걸린 시간 |
| `aps.schedule.execution.input.orders` | DistributionSummary | 없음 | 완료 결과에 포함된 서로 다른 생산오더 수 |
| `aps.schedule.execution.input.operations` | DistributionSummary | 없음 | 완료 결과에 포함된 서로 다른 공정 정의 수 |
| `aps.schedule.execution.output.tasks` | DistributionSummary | 없음 | 완료된 ScheduleRun에 생성된 작업 수 |
| `aps.schedule.execution.failures` | Counter | `stage` | 큐, 시작, 계산, 결과 연결 단계의 실패 수 |

실패 `stage`는 `queue`, `start`, `calculation`, `result_link` 네 값으로 고정합니다.
입력 규모 메트릭은 성공한 결과가 있어야 정확히 계산할 수 있으므로 완료 실행에만 기록합니다.
실행시간은 내부 큐 대기시간을 포함하지 않습니다.

## 3. 구조화 로그

로그는 수집 도구에 종속되지 않는 `key=value` 형식입니다.

```text
event=schedule_execution_completed executionId=31 resultScheduleRunId=41 outcome=success durationMs=125 orderCount=2 operationCount=3 taskCount=6
event=schedule_execution_failed executionId=31 outcome=failure failureStage=calculation failureType=ApplicationException durationMs=17
```

다음 값은 로그와 메트릭 태그에 기록하지 않습니다.

- `executionKey`
- 오더·품목·공정·설비의 코드와 이름
- 계획 시작시각과 동결 기준시각
- 요청 본문과 CSV 내용
- 예외 메시지와 저장된 사용자용 실패 사유

메트릭 태그도 `outcome`, 고정된 `stage`처럼 값 종류가 제한된 기술 정보만 사용합니다.
실행 ID와 결과 ID는 이력 조회를 위한 내부 기술 식별자로 로그에만 남기며 메트릭 태그로 사용하지 않습니다.

## 4. 운영 확인

기본 설정에서 Health와 Metrics 엔드포인트를 노출합니다.

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8080/actuator/metrics
Invoke-RestMethod "http://localhost:8080/actuator/metrics/aps.schedule.execution.duration?tag=outcome:success"
Invoke-RestMethod http://localhost:8080/actuator/metrics/aps.schedule.execution.input.orders
Invoke-RestMethod "http://localhost:8080/actuator/metrics/aps.schedule.execution.failures?tag=stage:calculation"
```

애플리케이션 자체에 인증·인가가 아직 없으므로 `/actuator/metrics`를 공개 인터넷에 직접 노출하지
않습니다. 운영 배포 시에는 내부 네트워크나 배포 환경의 접근 제어로 제한해야 합니다.

## 5. DB 쿼리 관찰

HikariCP 연결 풀 메트릭은 기본 Actuator 설정으로 확인할 수 있습니다.

```powershell
Invoke-RestMethod http://localhost:8080/actuator/metrics/hikaricp.connections.active
Invoke-RestMethod http://localhost:8080/actuator/metrics/hikaricp.connections.pending
```

Hibernate 쿼리 통계는 추가 비용이 있으므로 기본값이 `false`입니다. 문제 분석이나 성능 측정 시에만
활성화하고 애플리케이션을 재시작합니다.

```powershell
$env:APS_HIBERNATE_STATISTICS='true'
.\scripts\run-local.ps1

Invoke-RestMethod http://localhost:8080/actuator/metrics/hibernate.query.executions
Invoke-RestMethod "http://localhost:8080/actuator/metrics/hibernate.statements?tag=status:prepared"
```

`hibernate.query.executions`는 Hibernate 쿼리 실행 수, `hibernate.statements`의
`status=prepared`는 준비된 SQL 문 수를 누적해서 보여줍니다. 특정 실행 전후 값을 비교할 때는
다른 요청이 없는 단일 인스턴스에서 측정합니다. SQL 본문이나 바인딩 값은 메트릭에 저장하지 않습니다.

확인이 끝나면 환경변수를 제거하고 재시작합니다.

```powershell
Remove-Item Env:APS_HIBERNATE_STATISTICS
```

## 6. 검증과 제외 범위

자동 테스트는 다음을 검증합니다.

- 성공 실행시간과 오더·공정·작업 수 기록
- 고정 실패 단계별 카운터와 실패 실행시간 기록
- 성공·실패 구조화 로그 필드와 `executionKey` 비노출
- `/actuator/metrics`의 핵심 스케줄 메트릭 노출
- PostgreSQL 실제 쿼리와 `hibernate.query.executions` 증가

외부 APM, 로그 수집기, Prometheus 서버, 클라우드 모니터링, 대시보드와 경보 규칙은 추가하지 않습니다.
