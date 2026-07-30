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
| `machine` | Machine | Operation을 수행할 고정 설비 |

MVP에서는 Operation마다 하나의 설비만 지정합니다. 대체 설비, 병렬 공정, 작업자와
setup 시간은 첫 순방향 스케줄러 범위에서 제외하고 이후 제약조건 단계에서 확장합니다.

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
| `SchedulingOperationInput` | 공정·설비 ID, 순서, 단위 처리시간과 설비 근무시간 |
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
| `status` | ScheduleRunStatus | 저장 완료 시 `COMPLETED` |
| `scheduledOperations` | List | 실행에 포함된 작업 결과 |

PostgreSQL `TIMESTAMP WITH TIME ZONE`은 원래 offset을 보존하지 않으므로 `planningOffsetSeconds`를
별도로 저장합니다. 간트와 CAPA 조회는 이 값을 사용해 DB 재조회 후에도 공장 현지시각을 유지합니다.

같은 `executionKey`의 완료 결과가 있으면 새 결과를 만들지 않고 기존 실행을 반환합니다.

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
