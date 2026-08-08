# ERD

## 1. Current Schema

현재 스키마 기준은 Flyway `V18__create_schedule_execution.sql`입니다.
JPA는 `ddl-auto=validate`로 아래 테이블과 매핑의 일치 여부만 검증합니다.

```mermaid
erDiagram
    FACTORY {
        BIGINT factory_id PK
        VARCHAR_50 factory_code UK
        VARCHAR_100 factory_name
        BOOLEAN active
    }
    PRODUCTION_LINE {
        BIGINT production_line_id PK
        BIGINT factory_id FK
        VARCHAR_50 line_code
        VARCHAR_100 line_name
        BOOLEAN active
    }
    MACHINE {
        BIGINT machine_id PK
        BIGINT production_line_id FK
        VARCHAR_50 machine_code
        VARCHAR_100 machine_name
        VARCHAR_20 status
    }
    PRODUCT {
        BIGINT product_id PK
        VARCHAR_50 product_code UK
        VARCHAR_100 product_name
        VARCHAR_20 unit
        BOOLEAN active
    }
    ROUTING {
        BIGINT routing_id PK
        BIGINT product_id FK
        VARCHAR_50 routing_code
        VARCHAR_100 routing_name
        BOOLEAN active
    }
    OPERATION {
        BIGINT operation_id PK
        BIGINT routing_id FK
        BIGINT machine_id FK
        INTEGER operation_sequence
        VARCHAR_50 operation_code
        VARCHAR_100 operation_name
        INTEGER processing_time_minutes
    }
    OPERATION_MACHINE_CANDIDATE {
        BIGINT operation_machine_candidate_id PK
        BIGINT operation_id FK
        BIGINT machine_id FK
        INTEGER candidate_priority
    }
    PRODUCTION_ORDER {
        BIGINT production_order_id PK
        BIGINT routing_id FK
        VARCHAR_50 order_number UK
        BIGINT quantity
        TIMESTAMPTZ release_at
        TIMESTAMPTZ due_at
        INTEGER priority
        VARCHAR_20 status
    }
    WORKING_CALENDAR {
        BIGINT working_calendar_id PK
        BIGINT machine_id FK
        VARCHAR_9 day_of_week
        TIME start_time
        TIME end_time
        BOOLEAN active
    }
    CHANGEOVER_TIME {
        BIGINT changeover_time_id PK
        BIGINT machine_id FK
        BIGINT from_product_id FK
        BIGINT to_product_id FK
        INTEGER changeover_minutes
        BOOLEAN active
    }
    MACHINE_MAINTENANCE {
        BIGINT machine_maintenance_id PK
        BIGINT machine_id FK
        TIMESTAMPTZ start_at
        TIMESTAMPTZ end_at
        VARCHAR_200 maintenance_reason
        BOOLEAN active
    }
    SCHEDULE_RUN {
        BIGINT schedule_run_id PK
        UUID execution_key UK
        TIMESTAMPTZ planning_start
        TIMESTAMPTZ scheduling_end
        INTEGER planning_offset_seconds
        BIGINT source_schedule_run_id FK
        TIMESTAMPTZ frozen_at
        VARCHAR_30 dispatching_rule
        BIGINT total_tardiness_minutes
        INTEGER delayed_order_count
        BIGINT makespan_minutes
        NUMERIC_7_2 machine_utilization_percent
        TIMESTAMPTZ created_at
        VARCHAR_20 status
    }
    SCHEDULED_OPERATION {
        BIGINT scheduled_operation_id PK
        BIGINT schedule_run_id FK
        BIGINT production_order_id FK
        BIGINT operation_id FK
        BIGINT machine_id FK
        INTEGER operation_sequence
        TIMESTAMPTZ changeover_start_at
        BIGINT changeover_minutes
        TIMESTAMPTZ start_at
        TIMESTAMPTZ end_at
        BIGINT working_minutes
        BOOLEAN delayed
    }
    PLANNING_DATA_IMPORT_RUN {
        BIGINT planning_data_import_run_id PK
        UUID request_key UK
        VARCHAR_255 file_name
        VARCHAR_64 file_sha256
        INTEGER total_rows
        INTEGER success_rows
        INTEGER failed_rows
        INTEGER retry_count
        VARCHAR_20 status
        VARCHAR_500 failure_reason
        TIMESTAMPTZ created_at
        TIMESTAMPTZ started_at
        TIMESTAMPTZ completed_at
    }
    PLANNING_DATA_IMPORT_ROW {
        BIGINT planning_data_import_row_id PK
        BIGINT planning_data_import_run_id FK
        INTEGER row_number
        VARCHAR_30 data_type
        VARCHAR_20 status
    }
    PLANNING_DATA_IMPORT_ROW_ERROR {
        BIGINT planning_data_import_row_error_id PK
        BIGINT planning_data_import_row_id FK
        VARCHAR_50 error_field
        VARCHAR_50 error_code
        VARCHAR_500 error_message
    }
    SCHEDULE_EXECUTION {
        BIGINT schedule_execution_id PK
        UUID execution_key UK
        TIMESTAMPTZ planning_start
        INTEGER planning_offset_seconds
        VARCHAR_30 dispatching_rule
        BIGINT source_schedule_run_id FK
        TIMESTAMPTZ frozen_at
        BIGINT result_schedule_run_id FK
        VARCHAR_20 status
        VARCHAR_500 failure_reason
        TIMESTAMPTZ created_at
        TIMESTAMPTZ started_at
        TIMESTAMPTZ completed_at
    }
    FACTORY ||--o{ PRODUCTION_LINE : contains
    PRODUCTION_LINE ||--o{ MACHINE : contains
    PRODUCT ||--o{ ROUTING : defines
    ROUTING ||--|{ OPERATION : contains
    MACHINE ||--o{ OPERATION : primary_machine
    OPERATION ||--|{ OPERATION_MACHINE_CANDIDATE : allows
    MACHINE ||--o{ OPERATION_MACHINE_CANDIDATE : candidate
    ROUTING ||--o{ PRODUCTION_ORDER : produces
    MACHINE ||--o{ WORKING_CALENDAR : available
    MACHINE ||--o{ CHANGEOVER_TIME : configures
    MACHINE ||--o{ MACHINE_MAINTENANCE : unavailable
    PRODUCT ||--o{ CHANGEOVER_TIME : from_product
    PRODUCT ||--o{ CHANGEOVER_TIME : to_product
    SCHEDULE_RUN ||--o{ SCHEDULED_OPERATION : contains
    SCHEDULE_RUN o|--o{ SCHEDULE_RUN : source_of
    PRODUCTION_ORDER ||--o{ SCHEDULED_OPERATION : scheduled
    OPERATION ||--o{ SCHEDULED_OPERATION : plans
    MACHINE ||--o{ SCHEDULED_OPERATION : occupies
    PLANNING_DATA_IMPORT_RUN ||--o{ PLANNING_DATA_IMPORT_ROW : contains
    PLANNING_DATA_IMPORT_ROW ||--o{ PLANNING_DATA_IMPORT_ROW_ERROR : explains
    SCHEDULE_RUN o|--o{ SCHEDULE_EXECUTION : source_request
    SCHEDULE_RUN o|--o| SCHEDULE_EXECUTION : result_of
```

### 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `pk_factory` | `factory_id` | 공장 내부 식별자 |
| `uk_factory_code` | `factory_code` | 정규화된 공장 코드 중복 방지 |
| `ck_factory_code_format` | `factory_code` | 공장 코드 허용 문자와 길이 검증 |
| `ck_factory_name_not_blank` | `factory_name` | 공백으로만 구성된 이름 방지 |

애플리케이션 도메인 검증은 빠른 실패와 명확한 메시지를 담당하고, 데이터베이스 제약조건은 우회 입력과 동시 요청에서도 무결성을 보장합니다.

### ProductionLine 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `pk_production_line` | `production_line_id` | 생산라인 내부 식별자 |
| `fk_production_line_factory` | `factory_id` | 소속 Factory 참조 |
| `uk_production_line_factory_code` | `factory_id`, `line_code` | 공장 내 라인 코드 중복 방지 |
| `ck_production_line_code_format` | `line_code` | 라인 코드 형식 검증 |
| `ck_production_line_name_not_blank` | `line_name` | 공백 이름 방지 |
| `ix_production_line_factory_id` | `factory_id` | 공장별 라인 접근을 위한 인덱스 |

### Machine 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `pk_machine` | `machine_id` | 설비 내부 식별자 |
| `fk_machine_production_line` | `production_line_id` | 소속 생산라인 참조 |
| `uk_machine_line_code` | `production_line_id`, `machine_code` | 라인 내 설비 코드 중복 방지 |
| `ck_machine_code_format` | `machine_code` | 설비 코드 형식 검증 |
| `ck_machine_name_not_blank` | `machine_name` | 공백 이름 방지 |
| `ck_machine_status` | `status` | 정의된 MachineStatus만 허용 |
| `ix_machine_production_line_id` | `production_line_id` | 라인별 설비 접근 인덱스 |

### Product 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `pk_product` | `product_id` | 품목 내부 식별자 |
| `uk_product_code` | `product_code` | 정규화된 품목 코드 중복 방지 |
| `ck_product_code_format` | `product_code` | 품목 코드 형식 검증 |
| `ck_product_name_not_blank` | `product_name` | 공백 이름 방지 |
| `ck_product_unit` | `unit` | 정의된 ProductUnit만 허용 |

### Routing과 Operation 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `uk_routing_product_code` | `product_id`, `routing_code` | 품목별 Routing 코드 중복 방지 |
| `uk_operation_routing_sequence` | `routing_id`, `operation_sequence` | Routing 내 공정 순서 중복 방지 |
| `uk_operation_routing_code` | `routing_id`, `operation_code` | Routing 내 공정 코드 중복 방지 |
| `ck_operation_processing_time` | `processing_time_minutes` | 1~10080분 범위 보장 |
| `ix_operation_machine_id` | `machine_id` | 설비별 Operation 접근 인덱스 |
| `uk_operation_machine_candidate` | `operation_id`, `machine_id` | 같은 설비의 후보 중복 방지 |
| `ck_operation_machine_candidate_priority` | `candidate_priority` | 후보 우선순위를 1~1000으로 제한 |
| `ix_operation_machine_candidate_operation_priority` | 공정·우선순위·후보 ID | 공정별 후보 조회 순서 지원 |
| `ix_operation_machine_candidate_machine_id` | `machine_id` | 설비별 후보 정의 접근 지원 |

`V14`는 기존 `operation.machine_id`를 각 공정의 우선순위 1 후보로 백필합니다. 주 설비 FK는
041 이전 스케줄러와 기존 API 계약을 위해 유지합니다.

### ProductionOrder 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `uk_production_order_number` | `order_number` | 생산오더 번호 중복 방지 |
| `ck_production_order_quantity` | `quantity` | 1~1,000,000 수량 범위 |
| `ck_production_order_dates` | `release_at`, `due_at` | 납기가 투입 가능 시각 이후임을 보장 |
| `ck_production_order_priority` | `priority` | 1~100 우선순위 범위 |
| `ix_production_order_status_due` | `status`, `due_at` | 스케줄 대상 오더 조회 지원 |

### WorkingCalendar 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `uk_working_calendar_machine_window` | 설비·요일·시작·종료 | 동일 근무 구간 중복 방지 |
| `ck_working_calendar_day` | `day_of_week` | 유효한 요일만 허용 |
| `ck_working_calendar_time` | `start_time`, `end_time` | 종료가 시작보다 이후임을 보장 |
| `ix_working_calendar_machine_day` | 설비·요일·시작 | 설비 주간 캘린더 조회 지원 |

### ScheduleRun과 ScheduledOperation 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `uk_schedule_run_execution_key` | `execution_key` | 같은 실행 요청의 중복 저장 방지 |
| `ck_schedule_run_period` | 계획 시작·스케줄 종료 | 종료가 계획 시작보다 이전이 아님을 보장 |
| `ck_schedule_run_planning_offset` | `planning_offset_seconds` | UTC offset을 ±18시간 범위로 제한 |
| `fk_schedule_run_source` | `source_schedule_run_id` | 재스케줄링 원본 실행을 참조하고 삭제를 제한 |
| `ck_schedule_run_reschedule_trace` | 원본 실행·동결 기준시각 | 두 값의 동시 존재, 계획 시작 이후 동결, 자기 참조 방지 |
| `ix_schedule_run_source_id` | `source_schedule_run_id` | 원본별 재스케줄 실행 조회 지원 |
| `ck_schedule_run_dispatching_rule` | `dispatching_rule` | `EXPLICIT_PRIORITY`, `EDD`, `SPT`만 허용 |
| `ck_schedule_run_total_tardiness` | `total_tardiness_minutes` | 총 납기 지연시간이 0분 이상임을 보장 |
| `ck_schedule_run_delayed_order_count` | `delayed_order_count` | 지연 오더 수가 0건 이상임을 보장 |
| `ck_schedule_run_makespan` | `makespan_minutes` | Makespan이 0분 이상임을 보장 |
| `ck_schedule_run_machine_utilization` | `machine_utilization_percent` | 설비 가동률을 0~100%로 제한 |
| `uk_scheduled_operation_run_order_operation` | 실행·오더·공정 | 한 실행에서 같은 오더 공정 중복 방지 |
| `ck_scheduled_operation_period` | 시작·종료 | 작업 종료가 시작보다 이후임을 보장 |
| `ck_scheduled_operation_changeover_minutes` | `changeover_minutes` | Changeover Time이 0분 이상임을 보장 |
| `ck_scheduled_operation_changeover_period` | 전환 시작·가공 시작 | 전환이 있으면 시작시각이 가공 시작보다 이전임을 보장 |
| `ix_scheduled_operation_run_start` | 실행·시작 | 간트 보드 시간순 조회 지원 |
| `ix_scheduled_operation_machine_start` | 설비·시작 | 설비별 부하 조회 지원 |

`V15`는 기존 실행을 `EXPLICIT_PRIORITY`로 표시하고 저장 작업에서 총 납기 지연시간,
지연 오더 수와 Makespan을 백필합니다. 과거 실행은 당시 후보 설비의 가용시간 스냅샷을
복원할 수 없으므로 설비 가동률은 `0`을 유지합니다. 신규 실행은 선택한 규칙과 네 KPI를
계획 결과와 함께 저장합니다.

`V16`은 일반 실행에는 null인 `source_schedule_run_id`, `frozen_at`을 추가합니다.
재스케줄링 실행은 두 값을 함께 저장해 원본을 변경하지 않고 계획 계보를 남깁니다.

### PlanningDataImport 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `uk_planning_data_import_request_key` | `request_key` | 같은 요청 키의 중복 실행 방지 |
| `ck_planning_data_import_hash` | `file_sha256` | 소문자 SHA-256 64자리 형식 보장 |
| `ck_planning_data_import_counts` | 행 수·재시도 수 | 1~2,000행과 음수가 아닌 집계 보장 |
| `ck_planning_data_import_status` | 실행 상태 | `RUNNING`, `COMPLETED`, `FAILED`, `INTERRUPTED`만 허용 |
| `uk_planning_data_import_row_number` | 실행·행 번호 | 한 실행 안의 논리 CSV 행 중복 방지 |
| `ck_planning_data_import_row_type` | 데이터 타입 | 지원하는 여섯 행 타입 또는 null만 허용 |
| `ck_planning_data_import_row_status` | 행 상태 | `SUCCEEDED`, `FAILED`, `SKIPPED`만 허용 |
| `fk_planning_data_import_row_run` | 실행 FK | 실행 삭제 시 행 결과 함께 삭제 |
| `fk_planning_data_import_error_row` | 행 FK | 행 삭제 시 오류 결과 함께 삭제 |

`V17`은 실행, 행 결과, 행 오류를 분리합니다. CSV 도메인 데이터 반영과 실행 완료 전이는
같은 트랜잭션이고, 검증·반영 실패 결과는 롤백 후 별도 트랜잭션으로 저장합니다.

### ScheduleExecution 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `uk_schedule_execution_key` | `execution_key` | 동일 실행 요청 중복 방지 |
| `uk_schedule_execution_result` | `result_schedule_run_id` | 결과 한 건을 실행 하나에만 연결 |
| `fk_schedule_execution_source` | 원본 ScheduleRun | 재스케줄 요청의 원본 삭제 제한 |
| `fk_schedule_execution_result` | 결과 ScheduleRun | 완료 결과 삭제 제한 |
| `ck_schedule_execution_rule` | 배차 규칙 | 지원하는 세 규칙만 허용 |
| `ck_schedule_execution_planning_offset` | 계획 offset 초 | UTC offset을 ±18시간으로 제한 |
| `ck_schedule_execution_trace` | 원본·동결 기준 | 두 값의 동시 존재와 시간 범위 보장 |
| `ck_schedule_execution_status` | 실행 상태 | 네 수명주기 상태만 허용 |
| `ck_schedule_execution_lifecycle` | 상태별 시각·결과·실패 | 잘못된 중간 상태 조합 저장 방지 |

`V18`은 요청 이력을 성공 결과와 분리합니다. `RUNNING`은 결과 ScheduleRun 존재 여부로
재시작 복구하고, `QUEUED`는 생성 순서로 다시 배차합니다.

`V20`의 `schedule_execution_order_scope`는 비동기 실행이 선택한 생산오더 ID를 실행 이력과 함께
보존합니다. 행이 없으면 기존 계약인 전체 `CONFIRMED` 범위이고, 한 행 이상이면 그 오더만
계산합니다. 실행·오더 복합 기본 키가 중복 범위를 제거하며 오더 삭제는 실행 이력이 남아 있는 동안
제한됩니다.

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `pk_schedule_execution_order_scope` | 실행·생산오더 | 같은 오더의 범위 중복 방지 |
| `fk_schedule_execution_order_scope_execution` | `schedule_execution` | 실행 삭제 시 범위 함께 삭제 |
| `fk_schedule_execution_order_scope_order` | `production_order` | 실행 이력의 범위 참조 보존 |
| `ix_schedule_execution_order_scope_order` | `production_order_id` | 오더가 포함된 실행 조회 지원 |

### LearningScenario 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `uk_learning_scenario_request_key` | `request_key` | 동일 생성 요청의 중복 인스턴스 방지 |
| `uk_learning_scenario_namespace` | `namespace` | 인스턴스별 업무 코드 격리 |
| `ck_learning_scenario_status` | `status` | `READY`, `RESET`만 허용 |
| `fk_learning_scenario_entity_instance` | 추적 엔티티·인스턴스 | 인스턴스 삭제 시 추적 정보 함께 삭제 |
| `uk_learning_scenario_entity` | 인스턴스·종류·업무 ID | 같은 엔티티의 중복 추적 방지 |
| `idx_learning_scenario_entity_instance` | `scenario_instance_id` | 인스턴스 초기화 대상 조회 지원 |

`V19`는 실습 인스턴스와 이 인스턴스가 생성한 업무 엔티티 목록을 분리합니다. 추적 테이블은
다형 업무 테이블을 직접 외래 키로 묶지 않고 종류와 ID를 저장하며, 애플리케이션이 의존성 역순으로
삭제해 다른 인스턴스와 사용자 데이터를 건드리지 않습니다.

### ChangeoverTime 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `pk_changeover_time` | `changeover_time_id` | 전환시간 내부 식별자 |
| `fk_changeover_time_machine` | `machine_id` | 전환이 발생하는 설비 참조 |
| `fk_changeover_time_from_product` | `from_product_id` | 이전 품목 참조 |
| `fk_changeover_time_to_product` | `to_product_id` | 다음 품목 참조 |
| `uk_changeover_time_machine_products` | 설비·이전 품목·다음 품목 | 방향성 조합 중복 방지 |
| `ck_changeover_time_different_products` | 이전 품목·다음 품목 | 동일 품목 조합 저장 방지 |
| `ck_changeover_time_minutes` | `changeover_minutes` | 0분 이상 보장 |
| `ix_changeover_time_machine_id` | `machine_id` | 설비별 전환시간 조회 지원 |
| `ix_changeover_time_product_pair` | 이전 품목·다음 품목 | 품목 전환 조합 조회 지원 |

### MachineMaintenance 제약조건

| 이름 | 대상 | 설명 |
| --- | --- | --- |
| `pk_machine_maintenance` | `machine_maintenance_id` | 계획 정비 내부 식별자 |
| `fk_machine_maintenance_machine` | `machine_id` | 정비 대상 설비 참조 |
| `ck_machine_maintenance_period` | 시작·종료 | 종료가 시작보다 이후임을 보장 |
| `ck_machine_maintenance_reason_not_blank` | `maintenance_reason` | 공백 사유 저장 방지 |
| `ex_machine_maintenance_no_overlap` | 설비·정비 구간 | PostgreSQL GiST 배제 제약으로 동시 요청의 겹침까지 차단 |
| `ix_machine_maintenance_machine_start` | 설비·시작 | 설비별 정비시간 조회 지원 |
