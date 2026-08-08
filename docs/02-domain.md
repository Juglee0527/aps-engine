# Domain Model

## 1. Factory

Factory는 생산라인과 설비가 소속되는 최상위 생산 사업장입니다.

### 속성

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `id` | Long | PostgreSQL Identity로 생성되는 내부 식별자 |
| `code` | String | 외부 식별자, 최대 50자, 생성 후 변경 불가 |
| `name` | String | 표시 이름, 최대 100자 |
| `active` | boolean | 신규 생성 시 `true` |

### 공장 코드 정책

- 입력 앞뒤 공백을 제거하고 대문자로 정규화합니다.
- 첫 글자는 영문 대문자 또는 숫자여야 합니다.
- 이후 문자는 영문 대문자, 숫자, 하이픈(`-`), 밑줄(`_`)을 사용할 수 있습니다.
- 대소문자가 다른 동일 코드를 별도 공장으로 취급하지 않습니다.
- 데이터베이스 Unique Constraint로 중복을 최종 차단합니다.

이 정책은 사용자가 입력한 코드의 대소문자 차이 때문에 동일 공장이 중복 생성되는 것을 방지합니다.

### 상태와 행위

```text
Factory.create(code, name)
  → code와 name 검증 및 정규화
  → active = true

rename(name)
  → name 검증 및 변경

activate()
  → active = true

deactivate()
  → active = false
```

현재 삭제 정책과 REST API는 정의하지 않습니다. Factory 등록은 007, 조회는 008 단계에서 구현합니다.

## 2. ProductionLine

ProductionLine은 한 Factory 안에서 실제 생산 흐름을 구성하는 논리적 자원 그룹입니다.

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `id` | Long | PostgreSQL Identity로 생성 |
| `factory` | Factory | 필수, 생성 후 소속 변경 불가 |
| `code` | String | 공장 안에서 유일, 최대 50자 |
| `name` | String | 최대 100자 |
| `active` | boolean | 신규 생성 시 `true` |

- 코드는 Factory 코드와 같은 문자·대문자 정규화 정책을 사용합니다.
- 코드 중복 범위는 전체 시스템이 아니라 소속 Factory입니다.
- 같은 `LINE-01`을 서로 다른 Factory에서 사용할 수 있습니다.
- 비활성 Factory에는 새 ProductionLine을 등록할 수 없습니다.
- Factory 삭제 시 연쇄 삭제하지 않으며 현재 삭제 기능 자체를 제공하지 않습니다.

## 3. Machine

Machine은 ProductionLine에 소속되어 실제 Operation을 수행할 설비입니다.

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `id` | Long | PostgreSQL Identity로 생성 |
| `productionLine` | ProductionLine | 필수, 생성 후 소속 변경 불가 |
| `code` | String | 생산라인 안에서 유일, 최대 50자 |
| `name` | String | 최대 100자 |
| `status` | MachineStatus | 필수, 내부 기본 생성 메서드는 `AVAILABLE` |

### 상태

| 상태 | 의미 |
| --- | --- |
| `AVAILABLE` | 스케줄 배정 가능한 정상 상태 |
| `STOPPED` | 일시적으로 정지된 상태 |
| `INACTIVE` | 사용 중단 상태 |

```text
AVAILABLE → STOPPED  : stop()
STOPPED   → AVAILABLE: restart()
AVAILABLE → INACTIVE : deactivate()
STOPPED   → INACTIVE : deactivate()
INACTIVE  → AVAILABLE: reactivate()
```

정의되지 않은 전이는 예외로 차단합니다. CAPA, WorkingCalendar와 Maintenance 정보는 이후 전용 단계에서 추가합니다.

Factory, ProductionLine, Machine 코드에서 같은 정규화 규칙이 세 번째 반복되어 `BusinessCodeNormalizer`로 검증 함수만 공통화했습니다. JPA 필드는 단순 문자열로 유지해 값 객체·Converter 복잡도는 추가하지 않았습니다.

## 4. Product

Product는 생산오더와 Routing이 참조하는 생산 대상 품목입니다.

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `id` | Long | PostgreSQL Identity로 생성 |
| `code` | String | 시스템 전체에서 유일, 최대 50자, 생성 후 변경 불가 |
| `name` | String | 표시 이름, 최대 100자 |
| `unit` | ProductUnit | `PIECE`, `KILOGRAM`, `METER` 중 하나 |
| `active` | boolean | 신규 생성 시 `true` |

코드는 다른 기준정보와 같은 대문자 정규화 규칙을 사용합니다. 현재 APS 수직 MVP에서
수량과 공정시간을 계산할 기준 단위만 관리하며 BOM, 재고, 가격과 단위 환산은 포함하지 않습니다.

## 5. Routing과 Operation

Routing은 한 Product를 생산하는 공정 경로이며, Operation의 순서 있는 집합입니다.

### Routing

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `id` | Long | 내부 식별자 |
| `product` | Product | 필수, 활성 품목만 신규 Routing 등록 가능 |
| `code` | String | 품목 안에서 유일 |
| `name` | String | 최대 100자 |
| `active` | boolean | 신규 생성 시 `true` |
| `operations` | List | 하나 이상, sequence 오름차순 |

### Operation

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `sequence` | int | Routing 안에서 유일, 1 이상 |
| `code` | String | Routing 안에서 유일 |
| `name` | String | 최대 100자 |
| `processingTimeMinutes` | int | 제품 1단위당 표준 가공시간, 1~10080분 |
| `machine` | Machine | 기존 API·데이터 호환성을 위한 주 설비 |
| `machineCandidates` | Set | 하나 이상의 후보 설비와 우선순위, 설비 중복 불가 |

기존 단일 설비 Routing은 주 설비를 우선순위 1 후보 하나로 자동 등록해 API와 스케줄러 호환성을
유지합니다. 후보 목록을 명시하면 주 설비를 우선순위 1로 포함해야 하며, 다른 후보와 우선순위가
같아도 등록할 수 있습니다. 우선순위는 1~1000이고 값이 작을수록 먼저 비교합니다.

`INACTIVE` 설비는 신규 후보로 등록할 수 없습니다. 일시 정지 상태인 `STOPPED` 설비는 기준정보에서
제거하지 않고 후보로 보존하되 실제 계획 후보에서는 제외합니다. 스케줄러는 가용 후보 각각의
캘린더, 기존 부하, 선행 공정 종료, Maintenance와 Changeover를 반영한 완료시각을 비교합니다.
가장 이른 완료시각을 우선하고 동률이면 후보 우선순위, 설비 ID 오름차순으로 선택합니다.

## 6. ProductionOrder

ProductionOrder는 지정 Routing으로 일정 수량을 납기 내 생산해야 하는 스케줄링 입력입니다.

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `orderNumber` | String | 시스템 전체에서 유일 |
| `routing` | Routing | 활성 상태이며 Operation이 하나 이상 존재 |
| `quantity` | long | 1~1,000,000 |
| `releaseAt` | OffsetDateTime | 생산 투입 가능 시각 |
| `dueAt` | OffsetDateTime | releaseAt보다 이후인 납기시각 |
| `priority` | int | 1~100, 값이 클수록 우선 |
| `status` | ProductionOrderStatus | 신규 생성 시 `DRAFT` |

```text
DRAFT → CONFIRMED → SCHEDULED
```

사용자가 검토한 DRAFT 오더만 CONFIRMED로 전환할 수 있고 스케줄러는 CONFIRMED 오더만
입력으로 사용합니다. 취소 상태는 DB 계약에 예약했지만 취소 유스케이스는 현재 범위에 포함하지 않습니다.

## 7. WorkingCalendar

WorkingCalendar는 Machine의 반복 주간 근무시간 한 구간입니다.

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `machine` | Machine | 소속 설비 |
| `dayOfWeek` | DayOfWeek | 월요일~일요일 |
| `startTime` | LocalTime | 근무 시작 |
| `endTime` | LocalTime | 시작보다 이후, 자정 넘김 금지 |
| `active` | boolean | 신규 생성 시 `true` |

실제 날짜 구간은 `WorkingTimeCalculator`가 주간 규칙을 전개해 계산합니다. 상세 계산 정책은
`docs/06-capacity.md`에서 관리합니다.

## 8. Scheduling Input과 Result

스케줄링 알고리즘은 JPA Entity를 직접 수정하지 않고 실행 시점의 순수 Java 입력 스냅샷을 사용합니다.

| 모델 | 책임 |
| --- | --- |
| `SchedulingOrderInput` | 오더·품목 ID, 번호, 수량, 투입 가능시각, 납기, 우선순위와 공정 목록 |
| `SchedulingOperationInput` | 공정·설비 ID, 순서, 단위 처리시간, 설비 근무시간과 정비 비가용 구간 |
| `SchedulingChangeoverInput` | 설비·이전 품목·다음 품목과 방향성 전환시간 스냅샷 |
| `ScheduledTask` | 배정된 오더·공정·설비, 전환 시작·분, 가공 시작·종료·분과 납기 지연 여부 |
| `SchedulingPlan` | 계획 시작, 전체 종료와 작업 목록 |

필요 작업시간은 `생산수량 × 단위 처리시간`으로 계산합니다. 같은 설비의 작업은 겹치지 않고,
후속 공정은 선행 공정 종료 이후에 시작합니다. 다른 품목으로 바뀌면 가공 전에 방향성
Changeover Time을 같은 설비의 근무시간 안에서 먼저 배정합니다.

## 9. ScheduleRun

ScheduleRun은 한 번의 스케줄 실행과 결과 집합을 나타냅니다.

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `executionKey` | UUID | 클라이언트가 생성하며 시스템 전체에서 유일 |
| `planningStart` | OffsetDateTime | 실행 요청의 계획 시작 instant |
| `schedulingEnd` | OffsetDateTime | 마지막 작업 종료 instant |
| `planningOffsetSeconds` | int | 실행 당시 고정 UTC offset, ±18시간 |
| `createdAt` | OffsetDateTime | 결과 생성시각 |
| `sourceScheduleRun` | ScheduleRun? | 재스케줄링 원본 실행, 일반 실행은 null |
| `frozenAt` | OffsetDateTime? | 동결 기준시각, 원본 실행과 함께 존재 |
| `dispatchingRule` | DispatchingRule | `EXPLICIT_PRIORITY`, `EDD`, `SPT` |
| `totalTardinessMinutes` | long | 오더별 납기 초과분 합계 |
| `delayedOrderCount` | int | 완료시각이 납기를 초과한 오더 수 |
| `makespanMinutes` | long | 계획 시작부터 마지막 작업 종료까지 경과 분 |
| `machineUtilizationPercent` | BigDecimal | 선택 설비 총 부하 / 실행기간 총 가용분 |
| `status` | ScheduleRunStatus | 저장 완료 시 `COMPLETED` |
| `scheduledOperations` | List | 실행에 포함된 작업 결과 |

PostgreSQL `TIMESTAMP WITH TIME ZONE`은 원래 offset을 보존하지 않으므로 `planningOffsetSeconds`를
별도로 저장합니다. 간트와 CAPA 조회는 이 값을 사용해 DB 재조회 후에도 공장 현지시각을 유지합니다.

규칙과 KPI는 실행 시점 스냅샷으로 저장하므로 이후 근무시간이나 정비 기준정보가 바뀌어도 과거
비교 결과는 유지됩니다. 같은 `executionKey`의 완료 결과가 있으면 새 결과를 만들지 않고 기존
실행을 반환합니다.

재스케줄링은 원본 실행을 수정하지 않고 새 ScheduleRun을 만듭니다. Changeover 또는 가공 시작이
`frozenAt`보다 이른 작업은 경계와 겹치더라도 전체를 고정하며, 이후 작업만 다시 배치합니다.
취소 오더는 이미 고정된 작업만 유지하고 미래 작업을 제거하며, 새 `CONFIRMED` 오더는 동결
기준시각 이후 계획에 포함합니다.

## 10. ScheduledOperation

ScheduledOperation은 ScheduleRun에 저장되는 공정 단위 작업 결과입니다.

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `productionOrder` | ProductionOrder | 실행 당시 대상 오더 |
| `operation` | Operation | 배정된 공정 |
| `machine` | Machine | 작업이 점유하는 설비 |
| `sequence` | int | Routing 공정 순서 |
| `changeoverStartAt` | OffsetDateTime | 전환이 있을 때의 준비작업 시작, 없으면 null |
| `changeoverMinutes` | long | 실제 근무시간 기준 준비작업 분, 없으면 0 |
| `startAt`, `endAt` | OffsetDateTime | 종료가 시작보다 이후 |
| `workingMinutes` | long | 실제 필요한 작업시간, 1분 이상 |
| `delayed` | boolean | 작업 종료가 오더 납기를 초과했는지 여부 |

ScheduleRun 저장과 대상 ProductionOrder의 `SCHEDULED` 전환은 하나의 트랜잭션에서 처리합니다.
중간 저장이 실패하면 실행 결과와 오더 상태 변경을 모두 롤백합니다.

## 11. ChangeoverTime

ChangeoverTime은 한 설비에서 이전 품목 생산을 마치고 다음 품목 생산을 시작하기 전에 필요한
준비시간 기준정보입니다.

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `machine` | Machine | 전환이 발생하는 설비 |
| `fromProduct` | Product | 직전에 생산한 품목 |
| `toProduct` | Product | 다음에 생산할 품목 |
| `changeoverMinutes` | int | 0분 이상 |
| `active` | boolean | 신규 생성 시 `true` |

전환시간은 방향성을 가집니다. 같은 설비에서도 `A → B`와 `B → A`는 서로 다른 기준정보이며,
설비·이전 품목·다음 품목 조합은 하나만 등록할 수 있습니다.

- 동일 품목은 전환이 아니므로 항상 0분이며 별도 기준정보를 등록하지 않습니다.
- 서로 다른 품목의 매핑이 없으면 기본값 0분을 사용합니다.
- 실제 준비작업이 필요 없는 조합을 명시하기 위해 서로 다른 품목에는 0분을 등록할 수 있습니다.
- 스케줄러는 설비의 직전 배정 품목을 추적해 다음 가공 전에 방향성 전환시간을 배정합니다.
- 첫 배정, 동일 품목과 매핑이 없는 조합은 전환시간 0분으로 처리합니다.

## 12. MachineMaintenance

MachineMaintenance는 설비의 반복 근무시간 중 특정 날짜와 시간에 발생하는 계획 정비 예외입니다.
WorkingCalendar가 “평소 언제 일하는가”를 정의한다면 Maintenance는 “그중 언제 일할 수 없는가”를
정의하므로 두 모델을 분리합니다.

| 속성 | 타입 | 규칙 |
| --- | --- | --- |
| `machine` | Machine | 정비 대상 설비 |
| `startAt` | OffsetDateTime | 정비 시작 instant |
| `endAt` | OffsetDateTime | 시작보다 이후인 정비 종료 instant |
| `reason` | String | 공백이 아닌 200자 이하 사유 |
| `active` | boolean | 신규 생성 시 `true` |

- 같은 설비의 활성 정비 구간은 서로 겹칠 수 없습니다.
- `[startAt, endAt)` 반개방 구간을 사용하므로 한 정비의 종료와 다음 정비의 시작이 같으면 허용합니다.
- 근무시간 밖의 정비도 등록할 수 있지만 현재 CAPA에는 영향을 주지 않습니다. 이후 근무시간이 바뀌면
  겹치는 부분만 자동으로 비가용시간이 됩니다.
- CAPA 조회와 스케줄러는 공통 `WorkingTimeCalculator`에서 근무시간에서 정비 구간을 차감합니다.

## 13. PlannedLeadTime

PlannedLeadTime은 저장된 ScheduleRun을 기준으로 생산오더가 투입 가능해진 시점부터 계획상
완료될 때까지 걸리는 경과시간을 분해한 조회 모델입니다.

| 속성 | 설명 |
| --- | --- |
| `releaseAt` | Lead Time 계산 시작인 생산오더 투입 가능시각 |
| `completionAt` | 해당 오더의 마지막 ScheduledOperation 종료시각 |
| `plannedLeadTimeMinutes` | `releaseAt`부터 `completionAt`까지의 전체 경과 분 |
| `processingMinutes` | ScheduledOperation `workingMinutes` 합계 |
| `changeoverMinutes` | ScheduledOperation `changeoverMinutes` 합계 |
| `waitingMinutes` | 전체 Lead Time에서 가공시간과 Changeover Time을 뺀 분 |
| `operationCount` | 저장된 공정 결과 수 |

```text
계획 Lead Time = 가공시간 + Changeover Time + 대기시간
대기시간 = 계획 Lead Time - 가공시간 - Changeover Time
```

대기시간에는 최초 작업 전 큐 대기, 공정 사이 대기, 비근무시간, 휴무와 Maintenance가 포함됩니다.
ScheduledOperation이 없는 실행은 계산할 생산오더가 없으므로 빈 목록을 반환합니다. 통계, 예측과
실적 Lead Time은 현재 모델의 책임이 아닙니다.

## 14. PlanningDataImportPreview

PlanningDataImportPreview는 CSV를 DB에 반영하기 전에 구조·값·참조를 검증한 조회 모델입니다.

| 속성 | 설명 |
| --- | --- |
| `readyToApply` | 모든 행이 유효해 다음 반영 단계로 넘길 수 있는지 여부 |
| `totalRows` | 헤더와 빈 행을 제외한 데이터 행 수 |
| `validRows` | 성공 예상 행 수 |
| `invalidRows` | 실패 예상 행 수 |
| `rows` | 논리 행 번호, 타입, 정규화 값과 오류 목록 |

행 오류는 `field`, 안정적인 `code`, 사용자용 `message`로 구성합니다. 미리보기는 transient
도메인 객체로 실제 생성 규칙을 검증하지만 Repository `save`를 호출하지 않습니다.

## 15. PlanningDataImportRun

PlanningDataImportRun은 CSV 반영 요청의 멱등성과 복구 상태를 보존하는 Aggregate Root입니다.
`requestKey`는 실행을 식별하고 `fileSha256`은 같은 키에 다른 파일이 섞이는 것을 막습니다.

| 속성 | 설명 |
| --- | --- |
| `requestKey` | 클라이언트가 생성한 요청 고유 UUID |
| `fileName`, `fileSha256` | 입력 파일 식별 정보 |
| `totalRows`, `successRows`, `failedRows` | 전체·성공·실패 행 수 |
| `retryCount` | 중단 후 같은 파일로 재개한 횟수 |
| `status` | `RUNNING`, `COMPLETED`, `FAILED`, `INTERRUPTED` |
| `failureReason` | 검증 또는 DB 반영 실패의 실행 단위 사유 |
| `rows` | 종료된 실행의 행별 상태와 오류 |

```text
신규 요청 → RUNNING → COMPLETED
                  └→ FAILED
재시작 감지: RUNNING → INTERRUPTED → RUNNING(같은 키·같은 파일 재요청)
```

`COMPLETED`와 `FAILED`는 종료 상태입니다. 같은 키와 같은 파일을 다시 요청하면 저장된 결과를
그대로 반환하고, 같은 키에 다른 파일 해시가 들어오면 충돌로 거부합니다. `INTERRUPTED`만 같은
파일로 재시도할 수 있습니다.

행 결과 상태는 `SUCCEEDED`, `FAILED`, `SKIPPED`입니다. 전체 반영이 성공해야 모든 행을
`SUCCEEDED`로 기록합니다. 검증 오류나 DB 제약 위반이 있으면 실제 데이터는 모두 반영하지 않고,
원인 행은 `FAILED`, 함께 되돌리거나 반영하지 않은 행은 `SKIPPED`로 명시합니다.

## 16. ScheduleExecution

ScheduleExecution은 계산 요청의 수명주기를, ScheduleRun은 성공한 계산 결과를 담당합니다.
요청 이력과 결과를 분리해 실패한 실행도 조회하면서 ScheduleRun의 완료 결과 불변성을 유지합니다.

| 속성 | 설명 |
| --- | --- |
| `executionKey` | 클라이언트가 재요청에도 유지하는 고유 UUID |
| `planningStart`, `planningOffsetSeconds` | 계획 시작시각과 요청 당시 UTC offset |
| `dispatchingRule` | `EXPLICIT_PRIORITY`, `EDD`, `SPT` |
| `productionOrderIds` | 비어 있으면 전체 CONFIRMED, 값이 있으면 명시적 계획 범위 |
| `sourceScheduleRun`, `frozenAt` | 재스케줄링 요청일 때만 함께 존재 |
| `resultScheduleRun` | 성공 후 연결되는 단 하나의 결과 |
| `status` | `QUEUED`, `RUNNING`, `COMPLETED`, `FAILED` |
| `failureReason` | 실패 시 사용자에게 제공하는 정제된 사유 |
| `createdAt`, `startedAt`, `completedAt` | 대기·시작·종료 시각 |

```text
QUEUED → RUNNING → COMPLETED → ScheduleRun
                 └→ FAILED
QUEUED ──────────────────────→ FAILED (대기열 거부)
```

동일 `executionKey`와 동일 파라미터의 재요청은 기존 실행을 반환합니다. 계획 시작, 규칙,
생산오더 범위, 원본 실행 또는 동결 기준이 다르면 요청 충돌입니다. 명시적 범위는 중복 ID를
제거하고 범위 안의 모든 오더가 `CONFIRMED`일 때만 계산합니다. PostgreSQL이 offset을 UTC로 읽는 특성을
보완하기 위해 계획 offset 초를 함께 저장하고 작업자 입력을 원래 offset으로 복원합니다.

## 17. LearningScenarioInstance

LearningScenarioInstance는 반복 가능한 APS 실습 한 회차의 수명주기와 데이터 경계를 담당합니다.
시나리오 정의는 카탈로그에 고정하고, 인스턴스는 실행 시점의 다음 평일 08:00을 계획 시작으로
계산합니다. 따라서 문서의 날짜가 오래되어도 같은 상대 시간 구조로 실습할 수 있습니다.

| 속성 | 설명 |
| --- | --- |
| `requestKey` | 같은 생성 요청을 안전하게 재시도하기 위한 UUID |
| `scenarioKey` | 서버 카탈로그의 결정론적 시나리오 키 |
| `namespace` | 반복 실행 간 업무 코드 충돌을 막는 인스턴스 전용 접두사 |
| `status` | 사용할 수 있는 `READY`, 초기화가 끝난 `RESET` |
| `planningStart` | 생성 시점 기준 다음 평일 08:00 |
| `trackedEntityCount` | 해당 인스턴스가 생성해 초기화 대상이 된 엔티티 수 |

LearningScenarioEntity는 인스턴스가 만든 업무 엔티티의 종류와 ID만 추적합니다. 초기화는 실행 결과,
생산오더, 제약, 기준정보 순으로 추적된 ID만 삭제하므로 사용자가 직접 등록한 데이터와 다른 실습
인스턴스는 보존됩니다. 생성과 초기화는 각각 하나의 트랜잭션으로 처리됩니다.

Dispatching Rule 비교는 별도의 Aggregate를 저장하지 않는 읽기 모델입니다. 같은 인스턴스의
`CONFIRMED` 오더 스냅샷을 `EXPLICIT_PRIORITY`, `EDD`, `SPT`로 각각 계산해 작업 순서와 KPI를
반환하지만 ScheduleRun을 저장하거나 오더를 `SCHEDULED`로 바꾸지 않습니다. 추천 규칙은 총
지연시간, 지연 오더 수, Makespan 순으로 고르고 완전 동률이면 위 규칙 순서를 사용합니다. 사용자가
선택한 규칙만 일반 학습 스케줄 실행 경로를 거쳐 영속화됩니다.

제조 제약 영향 비교도 같은 비파괴 읽기 모델을 사용합니다. 기준 계획은 방향별 Changeover 입력,
정비 비가용시간과 대체 설비 후보를 제거하고 기본 설비만 사용합니다. 제약 적용 계획은 실제 DB의
전환시간·정비·Operation 후보 설비를 그대로 사용합니다. 두 결과의 작업 시각, 선택 설비, 전환시간,
지연 KPI를 함께 반환하며 비교 자체는 원본 엔티티를 변경하지 않습니다.

Frozen Horizon 학습 실행은 하나의 인스턴스 안에서 기준 ScheduleRun과 재계획 ScheduleRun을 모두
추적합니다. 기준 계획 뒤 취소된 오더의 시작 전 작업은 제외하고, 새 긴급오더와 계획 정비를 추가한
뒤 인스턴스가 추적하는 생산오더 ID만 신규 오더 후보로 허용합니다. 경계 전에 시작한 작업은 종료가
경계 이후여도 고정하며, 나머지는 선택한 규칙으로 다시 배치합니다. 결과 읽기 모델은 원본·결과의
동일 오더/Operation을 연결해 `FIXED`, `MOVED`, `EXCLUDED`, `NEW`와 변경 이유를 계산합니다.

대량 계획 조회에서는 ScheduleRun Aggregate 전체를 브라우저 응답으로 역직렬화하지 않습니다.
ScheduleRun 요약은 작업 컬렉션을 로딩하지 않고 KPI와 별도 집계한 오더·작업 수만 반환합니다.
ScheduledOperation 조회 모델은 실행 ID를 필수 경계로 두고 설비, 겹침 기간, 오더·공정 검색 조건과
페이지 크기 100건 제한을 적용합니다. 전체 계획 저장 계약과 화면 탐색 계약을 분리해 작업 수가 늘어도
DOM 크기가 입력 규모에 비례해 무한히 증가하지 않게 합니다.

학습 진도는 서버 Aggregate가 아닙니다. 사용자 계정이 없는 현재 범위에서는 브라우저
`localStorage`에 버전, 시나리오 키, 상태(`STARTED`, `ANALYZED`, `COMPLETED`, `NEEDS_REVIEW`),
인스턴스 ID와 갱신시각만 저장합니다. 로드할 때 연결된 LearningScenarioInstance가 여전히 `READY`인지
조회하고 없거나 `RESET`이면 `NEEDS_REVIEW`로 바꿉니다. 저장소 손상·차단과 일시적 조회 실패는
무시해 생산계획 기능의 성공 여부와 학습 부가기능의 상태를 분리합니다.

결과 코치는 서버 카탈로그의 읽기 모델입니다. 시나리오 목적과 관찰 포인트를 질문으로 바꾸고,
시나리오에서 실제로 읽을 KPI 의미만 선택해 정적 해설·다음 실험과 함께 반환합니다. 별도 상태를
저장하지 않으며 시나리오 키별 매핑은 자동 테스트로 고정합니다.
