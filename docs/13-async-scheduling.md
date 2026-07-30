# 비동기 스케줄 실행

## 1. 책임 분리

스케줄 요청과 계산 결과를 서로 다른 영속 모델로 관리합니다.

```text
HTTP 요청
  → ScheduleExecution QUEUED 커밋
    → 단일 내부 작업자에 실행 ID 전달
      → RUNNING 커밋
        → ScheduleRunService 계산·결과·오더 상태 원자적 커밋
          → ScheduleExecution COMPLETED와 결과 ID 연결
```

`ScheduleExecution`은 대기·실행·실패와 재시작 복구 이력이고, `ScheduleRun`은 성공한
스케줄과 작업 목록입니다. 계산 실패 때문에 빈 ScheduleRun을 만들지 않습니다.

## 2. API 흐름

`POST /api/v1/schedules`와 Frozen Horizon 재스케줄링은 `202 Accepted`와 실행 ID를 반환합니다.
클라이언트는 `Location` 또는 아래 API로 상태를 조회합니다.

```http
GET /api/v1/schedules/executions/{executionId}
GET /api/v1/schedules/executions?limit=20
```

`COMPLETED`의 `resultScheduleRunId`로 `GET /api/v1/schedules/{scheduleRunId}`를 조회하면
간트와 KPI를 포함한 전체 결과를 얻습니다. 웹 화면은 0.5초 간격, 최대 2분 동안 상태를 확인한 뒤
완료 결과를 다시 불러옵니다.

## 3. 상태와 실패

| 상태 | 의미 |
| --- | --- |
| `QUEUED` | 요청 이력 커밋 후 작업자 대기 |
| `RUNNING` | 작업자가 소유권을 얻어 계산 중 |
| `COMPLETED` | ScheduleRun 커밋과 결과 연결 완료 |
| `FAILED` | 계산 오류, 중단 또는 대기열 거부 |

예상 가능한 `ApplicationException`은 사용자 메시지를 실패 사유로 저장합니다. 그 밖의 내부 예외는
실행 이력에 일반 메시지만 저장하고, 서버 구조화 로그에는 민감한 입력이나 예외 메시지 대신
예외 유형과 실패 단계만 남깁니다.

## 4. 멱등성과 동시 실행 정책

- 같은 `executionKey`와 같은 계획 시작·규칙·재스케줄 파라미터는 기존 실행을 반환합니다.
- 같은 키에 다른 파라미터를 보내면 `409 SCHEDULE_EXECUTION_REQUEST_CONFLICT`입니다.
- DB의 `execution_key` 유일 제약이 동시에 들어온 요청에서도 이력 하나만 허용합니다.
- 애플리케이션 내부 작업자는 하나이며 FIFO로 실행합니다.
- 대기열은 100건입니다. 넘친 요청은 이력을 `FAILED`로 종료합니다.
- 계산과 결과 저장은 기존 `ScheduleRunService`의 단일 트랜잭션을 그대로 사용합니다.

단일 작업자는 같은 CONFIRMED 오더를 여러 계산이 동시에 선점하는 것을 막기 위한 현재 정책입니다.
여러 애플리케이션 인스턴스의 분산 소유권, 외부 브로커와 분산 잠금은 지원하지 않습니다.

## 5. 재시작 복구

시작 시 `RUNNING` 실행을 같은 `executionKey`의 ScheduleRun과 대조합니다.

- 결과가 이미 커밋됐으면 실행을 `COMPLETED`로 연결합니다.
- 결과가 없으면 계산 트랜잭션이 완료되지 않은 것으로 보고 `FAILED`로 종료합니다.
- 아직 시작하지 않은 `QUEUED`는 생성 순서대로 내부 작업자에 다시 전달합니다.

결과 트랜잭션 커밋 뒤 실행 연결 전에 프로세스가 종료돼도 이미 저장된 ScheduleRun을 찾아
성공 상태를 복구할 수 있습니다. 계산 도중 종료되면 결과·오더 상태 트랜잭션은 롤백됩니다.

## 6. 검증

- 도메인 상태 전이와 요청 파라미터 멱등 비교
- 신규 요청만 배차하고 중복 요청은 재배차하지 않는 서비스
- 정상·예상 실패·결과 연결 실패 작업자 경계
- 대기열 거부 실패 처리
- 시작 시 RUNNING 대조 후 QUEUED 재배차
- PostgreSQL 유일 제약, offset 복원, 결과 연결과 중단 실패
- 기존 동기 계산 서비스와 ScheduleRun JPA 회귀

실행시간, 입력·출력 규모, 실패 단계와 DB 쿼리 관찰 방법은
[스케줄 실행 관측성](14-observability.md)을 참고합니다.
