# Forward Scheduling

## 1. 현재 범위

첫 스케줄링 엔진은 확정된 입력 스냅샷을 받아 가능한 가장 이른 시각에 작업을 배치하는 순방향 방식입니다.
Spring과 데이터베이스에 의존하지 않는 순수 Java 코드로 구현해 알고리즘만 빠르게 검증할 수 있습니다.

```text
ProductionOrder
  → Priority Rule
    → Operation Sequence
      → Available Machine Candidates
        → Existing Load + Directional Changeover Time
          → Machine Working Calendar − Maintenance
            → Earliest Completion → Priority → Machine ID
              → ScheduledTask
```

## 2. 입력과 결과

`SchedulingOrderInput`은 생산오더 식별자, 수량, 투입 가능시각, 납기, 우선순위와 공정 목록을 가집니다.
`SchedulingOperationInput`은 공정, 기존 주 설비, 순서, 단위 처리시간과 후보 설비 목록을 가집니다.
`SchedulingMachineCandidateInput`은 후보 설비 ID, 우선순위, 근무시간과 계획 시작 이후의
Maintenance 비가용 구간 스냅샷을 가집니다.
`SchedulingChangeoverInput`은 설비, 이전 품목, 다음 품목과 방향성 전환시간 스냅샷을 가집니다.

기존 단일 설비 입력 생성자는 같은 설비를 우선순위 1 후보 하나로 변환하므로 이전 호출 계약과
계획 결과를 유지합니다.

필요 작업시간은 다음과 같이 계산합니다.

```text
필요 작업시간(분) = 생산수량 × 단위 처리시간(분)
```

결과인 `ScheduledTask`는 생산오더·공정·설비와 시작·종료시각, 실제 작업시간, 납기 지연 여부를 기록합니다.
전환이 있으면 `changeoverStartAt`과 `changeoverMinutes`를 별도로 기록해 가공시간과 구분합니다.

## 3. 배정 규칙

사용자는 실행 요청에서 하나의 규칙을 명시적으로 선택합니다.

| 규칙 | 정렬 순서 |
| --- | --- |
| `EXPLICIT_PRIORITY` | 우선순위 내림차순 → 납기 오름차순 → 오더 ID 오름차순 |
| `EDD` | 납기 오름차순 → 우선순위 내림차순 → 오더 ID 오름차순 |
| `SPT` | 총 가공시간 오름차순 → 납기 오름차순 → 우선순위 내림차순 → 오더 ID 오름차순 |

SPT의 총 가공시간은 `수량 × Routing 내 단위 가공시간 합계`입니다. 식별자까지 동률 처리에
포함하므로 같은 입력과 규칙은 항상 같은 오더 순서를 만듭니다. 규칙을 생략하면 기존 동작인
`EXPLICIT_PRIORITY`를 적용하며, 서버는 규칙을 자동 추천하거나 변경하지 않습니다.

각 공정의 가장 이른 시작 가능시각은 다음 값 중 가장 늦은 시각입니다.

- 계획 시작시각
- 생산오더 투입 가능시각
- 선행 공정 종료시각
- 해당 설비의 직전 작업 종료시각

각 후보 설비에 이 시작시각부터 Changeover와 가공을 가상 배정하고 실제 완료시각을 비교합니다.
후보 평가 중에는 설비 상태를 변경하지 않으며 최종 선택한 후보만 부하와 직전 품목을 갱신합니다.

1. 완료시각 오름차순
2. 후보 우선순위 오름차순
3. 설비 ID 오름차순

따라서 요청이나 DB 조회에서 후보 순서가 달라도 같은 입력은 같은 설비를 선택합니다.
`STOPPED`, `INACTIVE` 후보와 근무시간이 없는 후보는 실행 입력에서 제외합니다. 가용 상태 후보가
없으면 `MACHINE_UNAVAILABLE_FOR_SCHEDULING`, 근무시간을 가진 후보가 없으면
`WORKING_CALENDAR_REQUIRED`로 실패합니다.

설비에 먼저 배정된 품목이 있고 다음 품목이 다르면 해당 방향의 Changeover Time을 조회합니다.
첫 작업, 동일 품목과 매핑이 없는 조합은 0분입니다. 전환 준비는 제품이 투입 가능한 뒤에 시작하는
비선행 준비를 허용하지 않는 정책으로 두며, 가공과 동일하게 설비 근무시간 안에서만 배정합니다.
전환이 근무 종료를 넘으면 다음 근무일에 이어서 배정한 뒤 가공을 시작합니다.

실제 배정은 설비의 근무시간 안에서만 진행하며 비근무시간과 주말은 건너뜁니다.
활성 Maintenance와 겹치는 근무시간도 건너뛰며, 정비시간 밖에서 남은 가공 또는 Changeover를
이어서 배정합니다.
PostgreSQL이 오더 시각을 UTC offset으로 복원하더라도, 반복 근무시간은 실행 요청의
`planningStart` offset으로 정규화해 공장 현지시각 기준을 유지합니다.
Spring JSON 역직렬화도 요청 offset을 UTC로 자동 조정하지 않도록 명시적으로 설정합니다.
`ScheduleRun`에는 계획 offset 초를 함께 저장해 DB 재조회 후에도 간트와 CAPA가 같은 공장 현지시각을 사용합니다.

## 4. 계획 KPI

`ScheduleRun`은 적용 규칙과 아래 KPI를 실행 결과 스냅샷으로 저장합니다.

```text
총 납기 지연시간 = Σ max(0, 오더 완료시각 - 납기시각)
지연 오더 수 = 완료시각이 납기시각을 지난 오더 수
Makespan = 마지막 작업 종료시각 - 계획 시작시각
설비 가동률 = Σ(가공 분 + Changeover 분) / Σ 선택 설비 실제 가용 분 × 100
```

설비 실제 가용 분은 계획 시작부터 마지막 작업 종료까지 선택된 설비의 근무 캘린더에서
Maintenance를 제외해 계산합니다. 가동률은 전체 계획 기준 소수 둘째 자리 값이며, 후보로만
검토하고 선택되지 않은 설비는 분모에 포함하지 않습니다.

## 5. 보장사항

- 같은 설비의 작업은 서로 겹치지 않습니다.
- 후속 공정은 선행 공정이 끝난 뒤 시작합니다.
- Changeover와 가공은 같은 설비에서 순서대로 배정되어 서로 겹치지 않습니다.
- 가공과 Changeover는 계획 정비 구간과 겹치지 않습니다.
- 선택된 후보 설비가 `ScheduledOperation.machine`에 저장됩니다.
- 같은 입력은 같은 결과를 반환합니다.
- 작업시간 곱셈 오버플로와 근무시간 누락을 명시적으로 실패 처리합니다.

## 6. 제한사항

한 Operation을 여러 설비에 나누는 작업 분할, 후보 조합의 전역 최적화와 상용 Solver는
현재 범위에 없습니다. 현재 선택은 공정별 가장 이른 완료시각을 사용하는 지역 최선 방식입니다.

## 7. 실행과 저장

`ScheduleRunService`는 아래 흐름을 하나의 트랜잭션으로 실행합니다.

```text
CONFIRMED 오더 조회
  → Routing·Operation·후보 Machine 조회
    → 후보별 WorkingCalendar·Changeover Time·Maintenance 스냅샷 구성
      → ForwardScheduler 실행
        → ScheduleRun·ScheduledOperation 저장
          → 오더를 SCHEDULED로 변경
```

`executionKey`는 클라이언트가 생성하는 실행 식별자입니다.
완료된 키의 재요청은 기존 결과를 반환하며, DB 유니크 제약이 동시 중복 저장을 최종 방어합니다.
저장이나 상태 변경 중 하나라도 실패하면 전체 트랜잭션을 롤백하므로 부분 결과가 남지 않습니다.

저장된 결과는 실행 당시의 오더, 공정, 설비를 참조합니다.
현재는 마스터 수정 API가 없으므로 별도 이름 스냅샷을 중복 저장하지 않습니다.

Routing의 Operation과 후보 설비는 ordered `Set`으로 fetch join 행 증폭에 따른 중복을 방지합니다.
실행 서비스도 여러 ProductionOrder가 같은 Routing을 공유하는 경우를 위해 오더와 공정을 ID 기준으로
정규화해 같은 오더·공정이 중복 저장되지 않도록 방어합니다.

## 8. 계획 Lead Time

저장된 ScheduleRun의 생산오더별 Lead Time은 `releaseAt`부터 마지막 공정 종료까지의 경과 분입니다.
가공시간과 Changeover Time은 저장된 작업 분을 합산하고, 나머지를 계획 대기시간으로 계산합니다.

```text
waitingMinutes
  = Duration(releaseAt, completionAt)
  - Σ workingMinutes
  - Σ changeoverMinutes
```

따라서 하루 경계, 주말, 비근무시간과 Maintenance로 작업하지 못한 시간은 대기시간에 남습니다.
이는 계획 구조를 설명하기 위한 값이며 실제 생산실적이나 통계적 예측은 아닙니다.
