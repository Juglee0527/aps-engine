# API Contract

## 1. 응답 원칙

- HTTP 상태 코드를 오류 성격에 맞게 사용합니다.
- 성공 응답을 공통 객체로 감싸지 않습니다.
- 예상 가능한 오류는 안정적인 오류 코드와 사용자 메시지를 반환합니다.
- 예상하지 못한 예외의 클래스명, 스택 추적 및 내부 메시지를 응답에 노출하지 않습니다.
- 필드 검증 오류가 없더라도 `fieldErrors`는 `null`이 아닌 빈 배열을 반환합니다.

## 2. 오류 응답

```json
{
  "code": "INVALID_REQUEST",
  "message": "요청값이 올바르지 않습니다.",
  "fieldErrors": [
    {
      "field": "name",
      "reason": "이름은 필수입니다."
    }
  ]
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `code` | string | Y | 클라이언트가 분기 기준으로 사용할 안정적인 오류 코드 |
| `message` | string | Y | 사용자 또는 개발자가 이해할 수 있는 오류 설명 |
| `fieldErrors` | array | Y | 요청 필드별 검증 오류이며 없으면 빈 배열 |
| `fieldErrors[].field` | string | Y | 오류가 발생한 요청 필드 |
| `fieldErrors[].reason` | string | Y | 해당 필드가 거부된 이유 |

## 3. 공통 오류 코드

| 오류 코드 | HTTP 상태 | 사용 시점 |
| --- | --- | --- |
| `INVALID_REQUEST` | 400 | 요청 본문 파싱 실패 또는 입력값 검증 실패 |
| `RESOURCE_NOT_FOUND` | 404 | 식별자로 요청한 리소스가 존재하지 않음 |
| `CONFLICT` | 409 | 중복 또는 현재 상태와 충돌하는 요청 |
| `FACTORY_CODE_DUPLICATED` | 409 | 정규화된 공장 코드가 이미 존재함 |
| `FACTORY_NOT_FOUND` | 404 | 요청한 ID의 공장이 존재하지 않음 |
| `FACTORY_INACTIVE` | 409 | 비활성 공장에 생산라인 등록 시도 |
| `PRODUCTION_LINE_CODE_DUPLICATED` | 409 | 같은 공장 내 생산라인 코드 중복 |
| `PRODUCTION_LINE_NOT_FOUND` | 404 | 요청한 생산라인이 존재하지 않음 |
| `PRODUCTION_LINE_INACTIVE` | 409 | 비활성 생산라인에 설비 등록 시도 |
| `MACHINE_CODE_DUPLICATED` | 409 | 같은 생산라인 내 설비 코드 중복 |
| `MACHINE_NOT_FOUND` | 404 | 요청한 설비가 존재하지 않음 |
| `MACHINE_INACTIVE` | 409 | 비활성 설비를 Operation에 배정 |
| `MACHINE_UNAVAILABLE_FOR_SCHEDULING` | 409 | AVAILABLE이 아닌 설비를 스케줄링 |
| `PRODUCT_CODE_DUPLICATED` | 409 | 품목 코드 중복 |
| `PRODUCT_NOT_FOUND` | 404 | 요청한 품목이 존재하지 않음 |
| `PRODUCT_INACTIVE` | 409 | 비활성 품목에 Routing 등록 |
| `ROUTING_CODE_DUPLICATED` | 409 | 같은 품목 내 Routing 코드 중복 |
| `ROUTING_NOT_FOUND` | 404 | 요청한 Routing이 존재하지 않음 |
| `PRODUCTION_ORDER_NUMBER_DUPLICATED` | 409 | 생산오더 번호 중복 |
| `PRODUCTION_ORDER_NOT_FOUND` | 404 | 요청한 생산오더가 존재하지 않음 |
| `PRODUCTION_ORDER_STATUS_INVALID` | 409 | 허용되지 않은 오더 상태 전이 |
| `WORKING_CALENDAR_OVERLAP` | 409 | 설비 근무시간이 기존 구간과 겹침 |
| `WORKING_CALENDAR_REQUIRED` | 409 | 스케줄링 설비의 근무시간 누락 |
| `CONFIRMED_PRODUCTION_ORDER_REQUIRED` | 409 | 실행할 확정 생산오더 없음 |
| `SCHEDULE_RUN_NOT_FOUND` | 404 | 스케줄 실행 결과가 존재하지 않음 |
| `SCHEDULE_EXECUTION_DUPLICATED` | 409 | 동일 실행 키가 동시에 저장됨 |
| `SCHEDULE_EXECUTION_NOT_FOUND` | 404 | 비동기 스케줄 실행 요청이 존재하지 않음 |
| `SCHEDULE_EXECUTION_REQUEST_CONFLICT` | 409 | 동일 실행 키에 다른 요청 파라미터를 사용함 |
| `PLANNING_DATA_IMPORT_NOT_FOUND` | 404 | 계획 데이터 입력 실행이 존재하지 않음 |
| `PLANNING_DATA_IMPORT_REQUEST_CONFLICT` | 409 | 동일 요청 키에 다른 CSV 파일을 사용함 |
| `CHANGEOVER_TIME_DUPLICATED` | 409 | 같은 설비·이전 품목·다음 품목 조합이 이미 존재함 |
| `CHANGEOVER_TIME_NOT_FOUND` | 404 | 요청한 Changeover Time이 존재하지 않거나 비활성 상태임 |
| `MAINTENANCE_OVERLAP` | 409 | 같은 설비의 활성 정비시간이 겹침 |
| `MAINTENANCE_NOT_FOUND` | 404 | 요청한 정비시간이 존재하지 않거나 비활성 상태임 |
| `INTERNAL_ERROR` | 500 | 사전에 정의하지 못한 서버 내부 오류 |

표에는 현재 `ErrorCode`에 정의되어 실제 유스케이스에서 사용하는 코드만 기록합니다.

## 4. 예외 처리 흐름

```text
HTTP 요청
  → RequestBody 파싱 또는 Bean Validation 실패
    → INVALID_REQUEST
  → ApplicationException
    → ErrorCode에 정의된 HTTP 상태와 메시지
  → 존재하지 않는 HTTP 경로
    → RESOURCE_NOT_FOUND
  → 예상하지 못한 Exception
    → 서버 로그에 원본 예외 기록
    → INTERNAL_ERROR 일반 메시지만 응답
```

## 5. 알려진 범위

- 현재 계약은 JSON 요청 본문 검증을 우선 지원합니다.
- 경로 변수와 쿼리 매개변수의 메서드 검증 오류는 해당 API가 추가되는 단계에서 실제 실패 형태를 확인하고 확장합니다.
- 인증·인가 오류는 현재 프로젝트 범위에 포함하지 않습니다.

## 6. Factory API

### Factory 등록

```http
POST /api/v1/factories
Content-Type: application/json
```

```json
{
  "code": "factory-01",
  "name": "서울 공장"
}
```

성공 시 코드는 대문자로 정규화되며 `201 Created`와 생성된 리소스의 `Location`을 반환합니다.

```http
HTTP/1.1 201 Created
Location: /api/v1/factories/1
```

```json
{
  "id": 1,
  "code": "FACTORY-01",
  "name": "서울 공장",
  "active": true
}
```

| 상황 | HTTP 상태 | 오류 코드 |
| --- | --- | --- |
| 코드 또는 이름 검증 실패 | 400 | `INVALID_REQUEST` |
| 정규화된 코드 중복 | 409 | `FACTORY_CODE_DUPLICATED` |

Factory 조회, 수정 및 삭제 API는 현재 단계에 포함하지 않습니다.

### Factory 단건 조회

```http
GET /api/v1/factories/{factoryId}
```

- 존재하면 `200 OK`와 Factory 응답을 반환합니다.
- ID가 1보다 작거나 숫자가 아니면 `400 INVALID_REQUEST`를 반환합니다.
- 존재하지 않으면 `404 FACTORY_NOT_FOUND`를 반환합니다.

### Factory 목록 조회

```http
GET /api/v1/factories?page=0&size=20
```

| 매개변수 | 기본값 | 제약 |
| --- | --- | --- |
| `page` | 0 | 0 이상 |
| `size` | 20 | 1 이상 100 이하 |

```json
{
  "content": [
    {
      "id": 1,
      "code": "FACTORY-01",
      "name": "서울 공장",
      "active": true
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1,
  "first": true,
  "last": true
}
```

목록은 Factory ID 오름차순으로 고정합니다. 검색, 복합 필터와 사용자 지정 정렬은 현재 범위에 포함하지 않습니다.

## 7. ProductionLine API

### ProductionLine 등록

```http
POST /api/v1/factories/{factoryId}/production-lines
Content-Type: application/json
```

```json
{
  "code": "line-01",
  "name": "조립 라인"
}
```

```http
HTTP/1.1 201 Created
Location: /api/v1/factories/1/production-lines/10
```

```json
{
  "id": 10,
  "factoryId": 1,
  "code": "LINE-01",
  "name": "조립 라인",
  "active": true
}
```

| 상황 | HTTP 상태 | 오류 코드 |
| --- | --- | --- |
| 요청값 검증 실패 | 400 | `INVALID_REQUEST` |
| Factory 없음 | 404 | `FACTORY_NOT_FOUND` |
| Factory 비활성 | 409 | `FACTORY_INACTIVE` |
| 같은 Factory 내 코드 중복 | 409 | `PRODUCTION_LINE_CODE_DUPLICATED` |

### Factory별 ProductionLine 목록

```http
GET /api/v1/factories/{factoryId}/production-lines?page=0&size=20
```

- `page` 기본값은 0입니다.
- `size` 기본값은 20이며 1 이상 100 이하입니다.
- ProductionLine ID 오름차순으로 반환합니다.
- Factory가 없으면 `404 FACTORY_NOT_FOUND`를 반환합니다.

ProductionLine 단건 조회, 수정 및 삭제는 현재 범위에 포함하지 않습니다.

## 8. Machine API

### Machine 등록

```http
POST /api/v1/production-lines/{productionLineId}/machines
```

```json
{
  "code": "MACHINE-01",
  "name": "절단 설비",
  "status": "AVAILABLE"
}
```

상태는 `AVAILABLE`, `STOPPED`, `INACTIVE` 중 하나여야 합니다. 성공 시 `201 Created`, `/api/v1/machines/{machineId}` Location과 Machine 응답을 반환합니다.

### Machine 단건 조회

```http
GET /api/v1/machines/{machineId}
```

존재하지 않으면 `404 MACHINE_NOT_FOUND`를 반환합니다.

### ProductionLine별 Machine 목록

```http
GET /api/v1/production-lines/{productionLineId}/machines?page=0&size=20
```

- `page` 기본값은 0입니다.
- `size` 기본값은 20이며 1 이상 100 이하입니다.
- Machine ID 오름차순으로 반환합니다.
- ProductionLine이 없으면 `404 PRODUCTION_LINE_NOT_FOUND`를 반환합니다.

Machine 상태 변경, CAPA, Calendar 및 Maintenance API는 현재 범위에 포함하지 않습니다.

## 9. Product API

### Product 등록

```http
POST /api/v1/products
Content-Type: application/json
```

```json
{
  "code": "PRODUCT-01",
  "name": "완제품 A",
  "unit": "PIECE"
}
```

단위는 `PIECE`, `KILOGRAM`, `METER` 중 하나입니다. 성공 시 `201 Created`와
`/api/v1/products/{productId}` Location을 반환합니다.

### Product 단건 및 목록 조회

```http
GET /api/v1/products/{productId}
GET /api/v1/products?page=0&size=20
```

목록은 Product ID 오름차순으로 반환합니다. 존재하지 않는 ID는
`404 PRODUCT_NOT_FOUND`, 중복 코드는 `409 PRODUCT_CODE_DUPLICATED`를 반환합니다.

Product 수정, 삭제, BOM과 재고 API는 현재 범위에 포함하지 않습니다.

## 10. Routing API

### Routing 등록

```http
POST /api/v1/products/{productId}/routings
Content-Type: application/json
```

```json
{
  "code": "ROUTING-01",
  "name": "표준 Routing",
  "operations": [
    {
      "sequence": 10,
      "code": "CUT",
      "name": "절단",
      "processingTimeMinutes": 15,
      "machineId": 100,
      "machineCandidates": [
        {
          "machineId": 100,
          "priority": 1
        },
        {
          "machineId": 101,
          "priority": 2
        }
      ]
    }
  ]
}
```

- Operation은 하나 이상 필요합니다.
- `sequence`와 Operation 코드는 같은 Routing 안에서 중복될 수 없습니다.
- 비활성 Product와 비활성 Machine은 신규 Routing 정의에 사용할 수 없습니다.
- 표준 가공시간은 제품 1단위당 분 단위입니다.
- `machineId`는 기존 API·데이터 호환성을 위한 주 설비입니다.
- `machineCandidates`를 생략하면 주 설비 하나를 우선순위 1 후보로 등록합니다.
- 후보 목록을 명시하면 하나 이상이어야 하며 주 설비를 우선순위 1로 포함해야 합니다.
- 후보 설비는 중복될 수 없지만 1~1000 범위의 우선순위는 같을 수 있습니다.
- `INACTIVE` 설비는 등록할 수 없습니다. `STOPPED` 설비는 후보 정의에 보존합니다.

### Routing 조회

```http
GET /api/v1/routings/{routingId}
GET /api/v1/products/{productId}/routings
```

응답의 Operation은 sequence 오름차순으로 반환합니다. 각 Operation은 기존 `machineId`와 함께
후보별 `machineId`, `priority`, 현재 `status`를 `machineCandidates`로 반환합니다.
스케줄 실행 시에는 `AVAILABLE` 상태이고 근무시간이 있는 후보만 평가합니다. 가장 이른 완료시각,
후보 우선순위, 설비 ID 순으로 선택하며 실제 선택 설비는 Schedule 응답의 `machineId`에 기록됩니다.
Routing 수정과 버전 활성화 전환은 현재 범위에 포함하지 않습니다.

## 11. ProductionOrder API

### ProductionOrder 등록

```http
POST /api/v1/production-orders
Content-Type: application/json
```

```json
{
  "orderNumber": "PO-2026-001",
  "routingId": 20,
  "quantity": 10,
  "releaseAt": "2026-08-03T08:00:00+09:00",
  "dueAt": "2026-08-04T18:00:00+09:00",
  "priority": 80
}
```

신규 오더는 `DRAFT`로 생성됩니다. 우선순위는 1~100이며 값이 클수록 스케줄링 시
먼저 처리합니다.

### 조회와 확정

```http
GET /api/v1/production-orders/{productionOrderId}
GET /api/v1/production-orders?page=0&size=20
POST /api/v1/production-orders/{productionOrderId}/confirm
```

- DRAFT 오더만 CONFIRMED로 전환할 수 있습니다.
- 같은 상태 전환을 반복하면 `409 PRODUCTION_ORDER_STATUS_INVALID`를 반환합니다.
- 중복 오더 번호는 `409 PRODUCTION_ORDER_NUMBER_DUPLICATED`를 반환합니다.
- 수정, 삭제와 취소 API는 현재 범위에 포함하지 않습니다.

## 12. WorkingCalendar와 가용시간 API

### 근무시간 등록

```http
POST /api/v1/machines/{machineId}/working-calendars
Content-Type: application/json
```

```json
{
  "entries": [
    {
      "dayOfWeek": "MONDAY",
      "startTime": "08:00:00",
      "endTime": "17:00:00"
    }
  ]
}
```

같은 요일의 겹치는 시간대는 `409 WORKING_CALENDAR_OVERLAP`을 반환합니다.

### 근무시간과 기간 가용시간 조회

```http
GET /api/v1/machines/{machineId}/working-calendars
GET /api/v1/machines/{machineId}/availability?from=2026-08-03T08:00:00%2B09:00&to=2026-08-07T17:00:00%2B09:00
```

가용시간 응답은 총 `availableMinutes`와 날짜별 실제 `intervals`를 반환합니다.
조회 구간은 최대 366일이며 활성 계획 정비와 겹치는 부분은 가용 구간에서 제외합니다.

## 13. 스케줄 실행과 결과 조회 API

### 스케줄 실행

```http
POST /api/v1/schedules
Content-Type: application/json
```

```json
{
  "executionKey": "3cb6bb7e-6d18-4d9b-b314-54812025c401",
  "planningStart": "2026-07-27T08:00:00+09:00",
  "dispatchingRule": "EDD"
}
```

- `CONFIRMED` 생산오더만 실행 대상입니다.
- 공정 설비는 `AVAILABLE` 상태이며 근무 캘린더가 있어야 합니다.
- `dispatchingRule`은 `EXPLICIT_PRIORITY`, `EDD`, `SPT` 중 하나이며 생략하면
  `EXPLICIT_PRIORITY`를 적용합니다.
- 각 규칙은 명시적 우선순위, 납기, 총 가공시간을 각각 첫 정렬 기준으로 사용합니다.
  서버가 규칙을 자동 추천하거나 입력을 보고 임의로 바꾸지는 않습니다.
- 요청은 `ScheduleExecution`을 먼저 커밋하고 `202 Accepted`와 실행 ID를 반환합니다.
- 같은 `executionKey`와 같은 계획 시작·규칙은 기존 실행을 반환하고 다시 배차하지 않습니다.
- 같은 키에 다른 파라미터를 보내면 `409 SCHEDULE_EXECUTION_REQUEST_CONFLICT`입니다.
- 단일 내부 작업자가 FIFO로 계산하며 성공하면 대상 오더 상태와 ScheduleRun을 한 트랜잭션에 저장합니다.
- 응답 `Location`은 `/api/v1/schedules/executions/{executionId}`입니다.

```json
{
  "id": 31,
  "executionKey": "3cb6bb7e-6d18-4d9b-b314-54812025c401",
  "status": "QUEUED",
  "planningStart": "2026-07-27T08:00:00+09:00",
  "planningOffsetSeconds": 32400,
  "dispatchingRule": "EDD",
  "sourceScheduleRunId": null,
  "frozenAt": null,
  "resultScheduleRunId": null,
  "failureReason": null,
  "createdAt": "2026-07-30T18:00:00+09:00",
  "startedAt": null,
  "completedAt": null
}
```

### 비동기 실행 상태와 이력 조회

```http
GET /api/v1/schedules/executions/{executionId}
GET /api/v1/schedules/executions?limit=20
```

- 상태는 `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED`입니다.
- 최근 이력 `limit`은 1~100이고 생성시각·ID 내림차순입니다.
- 완료되면 `resultScheduleRunId`가 존재하고 실패하면 정제된 `failureReason`이 존재합니다.
- 존재하지 않는 ID는 `404 SCHEDULE_EXECUTION_NOT_FOUND`입니다.
- 애플리케이션 재시작 시 결과가 이미 커밋된 `RUNNING`은 `COMPLETED`, 결과가 없는 실행은
  `FAILED`로 복구하고 `QUEUED`는 다시 배차합니다.

### 스케줄 결과 조회

```http
GET /api/v1/schedules/latest
GET /api/v1/schedules/{scheduleRunId}
```

응답은 실행 상태와 기간, 적용 규칙, 계획 KPI와 간트 보드에 필요한 작업별
품목·공정·설비·시작·종료 정보를 반환합니다.

```json
{
  "id": 11,
  "executionKey": "3cb6bb7e-6d18-4d9b-b314-54812025c401",
  "status": "COMPLETED",
  "planningStart": "2026-07-29T23:00:00Z",
  "schedulingEnd": "2026-07-30T07:30:00Z",
  "planningOffsetSeconds": 32400,
  "createdAt": "2026-07-30T00:00:00Z",
  "sourceScheduleRunId": null,
  "frozenAt": null,
  "dispatchingRule": "EDD",
  "orderCount": 4,
  "taskCount": 12,
  "totalTardinessMinutes": 90,
  "delayedOrderCount": 2,
  "makespanMinutes": 510,
  "machineUtilizationPercent": 72.50,
  "tasks": [
    {
      "id": 101,
      "productionOrderId": 21,
      "orderNumber": "PO-2026-001",
      "productId": 20,
      "productCode": "PRODUCT-B",
      "productName": "완제품 B",
      "operationId": 31,
      "sequence": 10,
      "operationCode": "CUT",
      "operationName": "절단",
      "machineId": 41,
      "machineCode": "MACHINE-01",
      "machineName": "절단 설비",
      "changeoverStartAt": "2026-07-30T00:00:00Z",
      "changeoverMinutes": 30,
      "startAt": "2026-07-30T00:30:00Z",
      "endAt": "2026-07-30T01:30:00Z",
      "workingMinutes": 60,
      "dueAt": "2026-07-31T09:00:00Z",
      "delayed": false
    }
  ]
}
```

`planningStart`와 작업 시각은 DB 재조회 시 UTC로 반환될 수 있습니다.
클라이언트는 `planningOffsetSeconds`를 적용해 실행 당시 공장 현지시각으로 표시하고,
같은 offset을 설비 가용시간 조회에 전달해야 합니다.
`changeoverStartAt`은 다른 품목으로 전환하는 준비작업이 있을 때만 존재하며,
`changeoverMinutes`는 설비 근무시간 기준 실제 준비작업 분입니다.
`totalTardinessMinutes`는 오더별 `max(0, 완료시각 - 납기)`의 합이고,
`makespanMinutes`는 계획 시작부터 마지막 작업 종료까지의 경과 분입니다.
`machineUtilizationPercent`는 선택된 설비들의 실제 가용시간 대비
`(가공 분 + Changeover 분)` 비율이며 소수 둘째 자리까지 반환합니다.

### Frozen Horizon 재스케줄링

```http
POST /api/v1/schedules/{sourceScheduleRunId}/reschedule
Content-Type: application/json
```

```json
{
  "executionKey": "8743f2eb-5b06-43f8-ac75-31f0d43aaf0c",
  "frozenAt": "2026-07-30T10:00:00+09:00",
  "dispatchingRule": "SPT"
}
```

- `frozenAt`은 원본 실행의 계획 시작과 같거나 이후여야 합니다.
- Changeover 또는 가공 시작이 동결 기준보다 이른 작업은 진행 중이거나 경계와 겹쳐도
  원래 설비·시각 그대로 유지합니다.
- 기준시각 이후의 원본 작업만 다시 배치하고, 새 `CONFIRMED` 오더도 기준시각 이후에 포함합니다.
- `CANCELLED` 오더의 고정 작업은 이력으로 남기되 미래 작업은 새 실행에서 제외합니다.
- `dispatchingRule`을 생략하면 원본 실행의 규칙을 재사용합니다.
- 일반 실행과 동일하게 `202 Accepted`와 ScheduleExecution을 반환합니다.
- 실행 완료 후 결과 ScheduleRun의 `sourceScheduleRunId`, `frozenAt`으로 원본과 동결 기준을
  추적하며 원본 실행은 변경하지 않습니다.
- 수동 Drag & Drop과 실시간 MES 이벤트 반영은 이 API 범위에 없습니다.

## 14. Changeover Time API

### Changeover Time 등록

```http
POST /api/v1/machines/{machineId}/changeover-times
Content-Type: application/json
```

```json
{
  "fromProductId": 10,
  "toProductId": 20,
  "changeoverMinutes": 30
}
```

성공 시 `201 Created`, `/api/v1/changeover-times/{changeoverTimeId}` Location과 생성 결과를
반환합니다. 같은 품목 조합과 음수 시간은 `400 INVALID_REQUEST`, 같은 설비·이전 품목·다음 품목
조합 중복은 `409 CHANGEOVER_TIME_DUPLICATED`를 반환합니다.

### 단건 및 설비별 목록 조회

```http
GET /api/v1/changeover-times/{changeoverTimeId}
GET /api/v1/machines/{machineId}/changeover-times
```

- 단건 조회는 활성 기준정보만 반환하며 없으면 `404 CHANGEOVER_TIME_NOT_FOUND`를 반환합니다.
- 설비별 목록은 활성 기준정보를 이전 품목 ID, 다음 품목 ID 오름차순으로 반환합니다.
- 동일 품목의 전환시간과 등록되지 않은 방향성 조합은 스케줄링 정책에서 기본값 0분으로 해석합니다.
- 스케줄러는 활성 기준정보를 실행 시점 스냅샷으로 읽어 다음 가공 전에 반영합니다.

## 15. Machine Maintenance API

### 계획 정비 등록

```http
POST /api/v1/machines/{machineId}/maintenances
Content-Type: application/json
```

```json
{
  "startAt": "2026-08-03T10:00:00+09:00",
  "endAt": "2026-08-03T11:30:00+09:00",
  "reason": "월간 예방 점검"
}
```

성공하면 `201 Created`, `/api/v1/maintenances/{maintenanceId}` Location과 등록 결과를 반환합니다.
종료가 시작보다 이후가 아니면 `400 INVALID_REQUEST`, 같은 설비의 기존 활성 구간과 겹치면
`409 MAINTENANCE_OVERLAP`을 반환합니다.

### 단건 및 설비별 목록 조회

```http
GET /api/v1/maintenances/{maintenanceId}
GET /api/v1/machines/{machineId}/maintenances
```

- 목록은 정비 시작시각 오름차순입니다.
- 정비 구간은 `[startAt, endAt)`이므로 경계가 맞닿는 두 구간은 겹치지 않습니다.
- 근무시간 밖 정비도 저장하지만 가용시간과 스케줄에는 겹치는 부분만 반영합니다.

## 16. Planned Lead Time API

```http
GET /api/v1/schedules/{scheduleRunId}/lead-times
```

저장된 ScheduleRun의 생산오더별 계획 Lead Time을 생산오더 ID 오름차순으로 반환합니다.

```json
[
  {
    "productionOrderId": 20,
    "orderNumber": "PO-2026-001",
    "productId": 30,
    "productCode": "PRODUCT-A",
    "releaseAt": "2026-08-03T08:00:00+09:00",
    "completionAt": "2026-08-03T13:00:00+09:00",
    "plannedLeadTimeMinutes": 300,
    "processingMinutes": 120,
    "changeoverMinutes": 30,
    "waitingMinutes": 150,
    "operationCount": 2
  }
]
```

- 계산 시작은 생산오더 `releaseAt`, 완료는 마지막 저장 공정의 `endAt`입니다.
- 대기시간에는 작업 전·공정 사이 대기와 휴무, 비근무 및 정비시간이 포함됩니다.
- 저장 공정이 없는 실행은 빈 목록, 실행이 없으면 `404 SCHEDULE_RUN_NOT_FOUND`를 반환합니다.
- 이 API는 저장된 계획 결과의 산술 분해이며 통계나 예측값을 만들지 않습니다.

## 17. Bottleneck Detection API

```http
GET /api/v1/schedules/{scheduleRunId}/bottlenecks
```

저장된 ScheduleRun의 계획기간과 설비 부하를 기준으로 병목 후보를 반환합니다.

```json
{
  "scheduleRunId": 10,
  "from": "2026-08-03T08:00:00+09:00",
  "to": "2026-08-03T16:00:00+09:00",
  "thresholdPercent": 80.00,
  "candidates": [
    {
      "rank": 1,
      "machineId": 20,
      "machineCode": "MACHINE-A",
      "machineName": "조립 설비 A",
      "availableMinutes": 480,
      "loadMinutes": 420,
      "utilizationPercent": 87.50,
      "capacityExceeded": false,
      "reason": "HIGH_UTILIZATION"
    }
  ]
}
```

- 부하는 저장 작업의 `workingMinutes + changeoverMinutes`, 가용 분은 같은 기간의 근무시간에서
  Maintenance를 차감한 값입니다.
- 사용률 80% 이상을 후보로 판정합니다. 100% 초과는 `CAPACITY_EXCEEDED`입니다.
- 가용 분 0에 양의 부하가 있으면 사용률은 `null`, 사유는 `NO_AVAILABLE_CAPACITY`이며 최우선입니다.
- 이후 사용률 내림차순, 동일 사용률은 설비 코드와 ID 오름차순으로 순위를 고정합니다.
- 이 API는 진단 결과만 반환하며 설비 재배치나 스케줄 재실행을 수행하지 않습니다.

## 18. 계획 데이터 CSV 미리보기 API

```http
POST /api/v1/planning-data/imports/preview
Content-Type: multipart/form-data
```

`file` 파트에 UTF-8 CSV를 전송합니다. 최대 파일 크기는 2MB, 빈 행을 제외한 데이터는
최대 2,000행입니다. 브라우저에서는 `/planning-data-template.csv` 샘플을 내려받을 수 있습니다.

```json
{
  "readyToApply": false,
  "totalRows": 6,
  "validRows": 5,
  "invalidRows": 1,
  "rows": [
    {
      "rowNumber": 7,
      "type": "PRODUCTION_ORDER",
      "valid": false,
      "normalizedValues": {},
      "errors": [
        {
          "field": "routingCode",
          "code": "REFERENCE_NOT_FOUND",
          "message": "앞선 유효 행이나 DB에서 Routing을 찾을 수 없습니다."
        }
      ]
    }
  ]
}
```

- 행 번호는 헤더를 1번으로 보는 논리 CSV 행 번호입니다.
- 코드는 공백 제거 후 대문자로 정규화합니다.
- 행 타입은 `FACTORY → PRODUCTION_LINE → MACHINE/PRODUCT → ROUTING → PRODUCTION_ORDER`
  순서여야 합니다.
- Routing 한 행은 Operation 한 건이며 같은 Routing의 여러 행은 이름을 같게 유지하고
  Operation 순서와 코드를 중복할 수 없습니다.
- 참조는 앞선 유효 행 또는 기존 DB 기준정보에서 확인합니다.
- 열 개수 오류와 값·중복·참조 오류는 행별로 반환하고, 헤더·인코딩·파일 제한 오류는 `400`입니다.
- 이 API는 트랜잭션을 읽기 전용으로 사용하며 DB에 데이터를 반영하지 않습니다.

## 19. 계획 데이터 CSV 반영·이력 API

### 19.1 전체 반영

```http
POST /api/v1/planning-data/imports?requestKey={UUID}
Content-Type: multipart/form-data
```

미리보기와 같은 `file` 파트를 전송합니다. 검증을 다시 수행한 뒤 모든 행을 하나의
트랜잭션으로 반영합니다.

```json
{
  "id": 31,
  "requestKey": "0ee385d7-8f46-444d-84c2-f4075698063b",
  "fileName": "planning-data.csv",
  "fileSha256": "9a24a5d58e6c3ef336db56905d591e40f41c2fb52ea9990e0871634bac92e0fa",
  "status": "FAILED",
  "totalRows": 2,
  "successRows": 0,
  "failedRows": 1,
  "skippedRows": 1,
  "retryCount": 0,
  "failureReason": "CSV 검증 오류로 데이터를 반영하지 않았습니다.",
  "createdAt": "2026-07-30T17:00:00+09:00",
  "startedAt": "2026-07-30T17:00:00+09:00",
  "completedAt": "2026-07-30T17:00:00.050+09:00",
  "rows": [
    {
      "rowNumber": 2,
      "type": "FACTORY",
      "status": "SKIPPED",
      "errors": [
        {
          "field": "row",
          "code": "FILE_VALIDATION_FAILED",
          "message": "파일에 검증 오류가 있어 반영하지 않았습니다."
        }
      ]
    }
  ]
}
```

- 같은 `requestKey`와 같은 파일 해시는 새 데이터를 만들지 않고 기존 실행을 반환합니다.
- 같은 `requestKey`에 다른 파일을 보내면 `409 PLANNING_DATA_IMPORT_REQUEST_CONFLICT`입니다.
- 헤더·인코딩·파일 제한처럼 행 결과를 만들 수 없는 구조 오류는 실행을 생성하지 않고
  `400 INVALID_REQUEST`입니다.
- 검증 실패와 DB 제약 위반은 실행 결과를 저장했으므로 HTTP `200`과 `FAILED` 상태로 반환합니다.
- 반영 성공은 모든 행이 `SUCCEEDED`인 `COMPLETED`입니다.
- 한 행이라도 DB 반영에 실패하면 계획 데이터 변경을 모두 롤백하고, 원인 행은
  `DB_APPLY_FAILED`, 나머지는 `TRANSACTION_ROLLED_BACK`으로 기록합니다.
- 동시에 들어온 동일 요청은 먼저 생성된 실행을 기준으로 하나만 반영합니다.

### 19.2 실행 이력 조회

```http
GET /api/v1/planning-data/imports/{importRunId}
```

저장된 실행과 행 결과를 반환합니다. 존재하지 않으면
`404 PLANNING_DATA_IMPORT_NOT_FOUND`입니다.

애플리케이션 시작 시 남아 있는 `RUNNING`은 `INTERRUPTED`로 바꿉니다. 사용자가 같은 요청 키와
같은 파일을 다시 전송하면 실행 ID를 유지한 채 `retryCount`를 올리고 처음부터 원자적으로
재시도합니다. `COMPLETED`와 `FAILED`는 재실행하지 않습니다.

## 20. APS 학습 시나리오 API

### 20.1 시나리오 카탈로그 조회

```http
GET /api/v1/learning/scenarios
```

서버가 지원하는 실습의 키, 과정, 설명과 예상 설비·품목·오더 수를 반환합니다. 051에서는
`FIRST_PLAN` 정의를 제공하며 후속 실습 데이터 팩은 053~057에서 순차 추가합니다.

### 20.2 실습 인스턴스 생성

```http
POST /api/v1/learning/scenarios/FIRST_PLAN/instances
Content-Type: application/json

{"requestKey":"ad31b6e5-2621-4b90-a8f4-22430a640d96"}
```

```json
{
  "id": 7,
  "requestKey": "ad31b6e5-2621-4b90-a8f4-22430a640d96",
  "scenarioKey": "FIRST_PLAN",
  "namespace": "LS-7C6E6EAE",
  "status": "READY",
  "planningStart": "2026-08-10T08:00:00+09:00",
  "createdAt": "2026-08-08T14:00:00+09:00",
  "trackedEntityCount": 0
}
```

계획 시작은 서버 시간대의 다음 평일 08:00입니다. 같은 `requestKey`와 같은 시나리오는 기존
인스턴스를 반환하고, 다른 시나리오에 같은 키를 사용하면
`409 LEARNING_SCENARIO_REQUEST_CONFLICT`입니다.

### 20.3 인스턴스 조회와 초기화

```http
GET /api/v1/learning/instances/{instanceId}
DELETE /api/v1/learning/instances/{instanceId}
```

`DELETE`는 인스턴스가 추적한 데이터만 외래 키 안전 순서로 제거하고 상태를 `RESET`으로 바꿉니다.
이미 초기화한 인스턴스에 다시 요청해도 같은 결과를 반환합니다. 051은 인스턴스·추적·초기화
경계를 마련한 단계이며, 실제 기준정보와 생산오더 생성은 053부터 연결합니다.
