# 도메인 모델

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
| `status` | MachineStatus | 신규 생성 시 `AVAILABLE` |

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
