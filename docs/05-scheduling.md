# Forward Scheduling

## 1. 현재 범위

첫 스케줄링 엔진은 확정된 입력 스냅샷을 받아 가능한 가장 이른 시각에 작업을 배치하는 순방향 방식입니다.
Spring과 데이터베이스에 의존하지 않는 순수 Java 코드로 구현해 알고리즘만 빠르게 검증할 수 있습니다.

```text
ProductionOrder
  → Priority Rule
    → Operation Sequence
      → Machine Working Calendar
        → ScheduledTask
```

## 2. 입력과 결과

`SchedulingOrderInput`은 생산오더 식별자, 수량, 투입 가능시각, 납기, 우선순위와 공정 목록을 가집니다.
`SchedulingOperationInput`은 공정, 설비, 순서, 단위 처리시간과 설비 근무시간 스냅샷을 가집니다.

필요 작업시간은 다음과 같이 계산합니다.

```text
필요 작업시간(분) = 생산수량 × 단위 처리시간(분)
```

결과인 `ScheduledTask`는 생산오더·공정·설비와 시작·종료시각, 실제 작업시간, 납기 지연 여부를 기록합니다.

## 3. 배정 규칙

생산오더는 아래 순서로 정렬합니다.

1. 명시적 우선순위 내림차순
2. 납기시각 오름차순
3. 생산오더 식별자 오름차순

명시적 우선순위를 첫 기준으로 선택한 이유는 긴급오더를 운영자가 직접 제어할 수 있게 하기 위해서입니다.
납기와 식별자는 결과를 항상 동일하게 만드는 동률 처리 기준입니다.
정렬 정책은 `SchedulingPriorityRule` 하나로만 분리해 이후 규칙 교체 지점은 열어두되 전략 계층은 더 만들지 않았습니다.

각 공정의 가장 이른 시작 가능시각은 다음 값 중 가장 늦은 시각입니다.

- 계획 시작시각
- 생산오더 투입 가능시각
- 선행 공정 종료시각
- 해당 설비의 직전 작업 종료시각

실제 배정은 설비의 근무시간 안에서만 진행하며 비근무시간과 주말은 건너뜁니다.
PostgreSQL이 오더 시각을 UTC offset으로 복원하더라도, 반복 근무시간은 실행 요청의
`planningStart` offset으로 정규화해 공장 현지시각 기준을 유지합니다.
Spring JSON 역직렬화도 요청 offset을 UTC로 자동 조정하지 않도록 명시적으로 설정합니다.
`ScheduleRun`에는 계획 offset 초를 함께 저장해 DB 재조회 후에도 간트와 CAPA가 같은 공장 현지시각을 사용합니다.

## 4. 보장사항

- 같은 설비의 작업은 서로 겹치지 않습니다.
- 후속 공정은 선행 공정이 끝난 뒤 시작합니다.
- 같은 입력은 같은 결과를 반환합니다.
- 작업시간 곱셈 오버플로와 근무시간 누락을 명시적으로 실패 처리합니다.

## 5. 제한사항

현재 엔진에는 Changeover Time, Maintenance, 병렬 설비 선택, 작업 분할 정책과 최적화 탐색이 없습니다.

## 6. 실행과 저장

`ScheduleRunService`는 아래 흐름을 하나의 트랜잭션으로 실행합니다.

```text
CONFIRMED 오더 조회
  → Routing·Operation·Machine 조회
    → WorkingCalendar 스냅샷 구성
      → ForwardScheduler 실행
        → ScheduleRun·ScheduledOperation 저장
          → 오더를 SCHEDULED로 변경
```

`executionKey`는 클라이언트가 생성하는 실행 식별자입니다.
완료된 키의 재요청은 기존 결과를 반환하며, DB 유니크 제약이 동시 중복 저장을 최종 방어합니다.
저장이나 상태 변경 중 하나라도 실패하면 전체 트랜잭션을 롤백하므로 부분 결과가 남지 않습니다.

저장된 결과는 실행 당시의 오더, 공정, 설비를 참조합니다.
현재는 마스터 수정 API가 없으므로 별도 이름 스냅샷을 중복 저장하지 않습니다.

여러 ProductionOrder가 같은 Routing을 공유하면 JPA collection fetch 결과에 같은 Operation이
중복 materialize될 수 있습니다. 실행 서비스는 오더와 공정을 ID 기준으로 정규화한 뒤 알고리즘 입력을
구성해 같은 오더·공정이 중복 저장되지 않도록 방어합니다.
