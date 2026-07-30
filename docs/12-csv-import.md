# 계획 데이터 CSV 입력

## 1. 현재 범위

현재 구현은 CSV 미리보기, 원자적 반영, 요청 멱등성, 실행 이력과 중단 복구까지 포함합니다.

```text
UTF-8 CSV
  → 파일·헤더 검사
    → 코드 정규화와 행 타입별 값 검사
      → 앞선 유효 행·기존 DB 참조 확인
        → 미리보기
          → 요청 키·파일 해시 확인
            → 전체 행을 한 트랜잭션으로 반영
              → 실행·행별 결과 저장
```

미리보기는 `@Transactional(readOnly = true)`로 실행하며 Repository `save`를 호출하지 않습니다.
반영은 검증을 서버에서 다시 수행하므로 클라이언트의 미리보기 결과를 신뢰하지 않습니다.

## 2. 파일 제한

| 항목 | 정책 |
| --- | --- |
| 형식 | UTF-8 CSV, BOM 허용 |
| 파일 크기 | 최대 2MB |
| 데이터 행 | 헤더·빈 행 제외 최대 2,000행 |
| CSV 표현 | 쉼표 구분, 큰따옴표 필드와 `""` 이스케이프 지원 |
| 구조 오류 | 빈 파일, 헤더 누락·중복·알 수 없는 열, 잘못된 UTF-8, 닫히지 않은 따옴표는 `400` |
| 행 오류 | 열 개수, 필수값, 형식, 중복, 참조 오류는 해당 행 결과로 반환 |

서버 Multipart 상한은 프로토콜 오버헤드를 고려해 4MB이고, 유스케이스가 실제 CSV 파일을
2MB로 다시 제한합니다.

## 3. 고정 헤더

```text
type,factoryCode,lineCode,machineCode,productCode,routingCode,orderNumber,name,status,unit,operationSequence,operationCode,operationName,processingTimeMinutes,quantity,releaseAt,dueAt,priority
```

헤더 순서는 바꿀 수 있지만 모든 열이 한 번씩 존재해야 하며 알 수 없는 열은 허용하지 않습니다.
사용자 가이드 또는 `/planning-data-template.csv`에서 동일 계약의 샘플을 받을 수 있습니다.

## 4. 행 타입과 필수값

| type | 필수값 |
| --- | --- |
| `FACTORY` | `factoryCode`, `name` |
| `PRODUCTION_LINE` | `factoryCode`, `lineCode`, `name` |
| `MACHINE` | `factoryCode`, `lineCode`, `machineCode`, `name`, `status` |
| `PRODUCT` | `productCode`, `name`, `unit` |
| `ROUTING` | 공장·라인·설비·품목·Routing 코드, `name`, Operation 순서·코드·이름·가공시간 |
| `PRODUCTION_ORDER` | `productCode`, `routingCode`, `orderNumber`, 수량·투입·납기·우선순위 |

Routing 행 하나는 Operation 하나를 나타냅니다. 같은 `productCode + routingCode`를 반복하면
하나의 Routing에 Operation을 순서대로 추가합니다.

## 5. 정규화와 값 범위

- 모든 비즈니스 코드는 trim 후 대문자로 바꾸며 영문·숫자·하이픈·밑줄과 최대 50자를 허용합니다.
- 이름과 Operation 이름은 trim하고 최대 100자로 제한합니다.
- `status`: `AVAILABLE`, `STOPPED`, `INACTIVE`
- `unit`: `PIECE`, `KILOGRAM`, `METER`
- Operation 순서: 1 이상 정수
- 단위 가공시간: 1~10,080분
- 생산수량: 1~1,000,000
- 우선순위: 1~100
- 투입·납기: UTC offset을 포함한 ISO-8601이며 납기가 투입시각보다 이후여야 합니다.

## 6. 참조와 중복 정책

행은 아래 순서로 정렬합니다.

```text
FACTORY
  → PRODUCTION_LINE
    → MACHINE / PRODUCT
      → ROUTING
        → PRODUCTION_ORDER
```

참조 대상은 앞서 검증에 성공한 행이나 기존 DB에서 찾습니다. 파일 안에서 검증에 실패한 행은
후속 행의 참조 대상으로 사용하지 않습니다. 새로 생성하려는 비즈니스 키가 DB 또는 같은 파일에
이미 있으면 `DUPLICATE`입니다. 단, 같은 신규 Routing의 Operation 행 반복은 허용하며 Operation
순서와 코드는 Routing 안에서 고유해야 합니다.

## 7. 미리보기 오류 계약

| code | 의미 |
| --- | --- |
| `COLUMN_COUNT_MISMATCH` | 행 열 개수가 헤더와 다름 |
| `INVALID_TYPE` | 지원하지 않는 type |
| `REFERENCE_ORDER_INVALID` | 행 타입 순서 위반 |
| `REQUIRED` | 타입별 필수값 누락 |
| `INVALID_VALUE` | 코드·enum·숫자·시각·도메인 규칙 위반 |
| `REFERENCE_NOT_FOUND` | 앞선 유효 행과 DB 모두에서 참조를 찾지 못함 |
| `DUPLICATE` | DB 또는 파일 내부 비즈니스 키 중복 |

오류가 하나라도 있으면 `readyToApply=false`입니다. 사용자는 행 번호와 필드를 수정한 뒤 같은
미리보기 API로 다시 검증합니다.

## 8. 반영 흐름과 원자성

```http
POST /api/v1/planning-data/imports?requestKey={UUID}
```

`requestKey`는 사용자가 파일을 선택한 시점에 한 번 생성하고, 네트워크 오류로 같은 요청을
재전송할 때 유지합니다. 서버는 다음 순서로 처리합니다.

1. 2MB 제한 안의 파일을 읽고 SHA-256을 계산합니다.
2. 같은 요청 키의 실행이 있으면 파일 해시를 비교합니다.
3. 신규 또는 재시도 가능한 실행이면 CSV를 다시 검증합니다.
4. 행 검증 결과가 만들어지면 `RUNNING` 실행 이력을 생성하거나 중단 실행을 재개합니다.
5. 검증 오류가 있으면 실제 계획 데이터는 저장하지 않고 행 결과만 `FAILED`로 종료합니다.
6. 검증을 통과하면 Factory부터 ProductionOrder까지 파일 순서대로 반영합니다.
7. 모든 행 반영과 `COMPLETED` 전이를 같은 트랜잭션으로 커밋합니다.
8. DB 제약 위반이 발생하면 계획 데이터 변경을 모두 롤백한 뒤 실패 이력을 별도 트랜잭션으로 저장합니다.

행 단위 커밋이나 부분 성공은 허용하지 않습니다. 선행 기준정보만 저장되고 후속 생산오더가
누락되는 상태보다 사용자가 파일을 수정해 전체를 다시 실행할 수 있는 상태가 더 명확하기 때문입니다.
헤더·인코딩·파일 크기처럼 행 결과 자체를 만들 수 없는 구조 오류는 실행 이력을 만들지 않고
`400 INVALID_REQUEST`로 반환합니다.

## 9. 멱등성 정책

| 기존 실행 | 같은 요청 키·같은 파일 | 같은 요청 키·다른 파일 |
| --- | --- | --- |
| 없음 | 새 `RUNNING` 생성 후 반영 | 해당 없음 |
| `RUNNING` | 기존 실행 반환, 중복 반영 안 함 | `409` |
| `COMPLETED` | 저장된 성공 결과 반환 | `409` |
| `FAILED` | 저장된 실패 결과 반환 | `409` |
| `INTERRUPTED` | 같은 실행 ID로 처음부터 재시도 | `409` |

DB의 `request_key` 유일 제약이 동시 요청에서도 실행 하나만 생성되도록 보장합니다.
파일 내용이 같으면 파일 이름이 달라도 동일한 입력으로 판단합니다. 실패한 파일을 수정했다면
새로운 요청 키를 사용해야 합니다.

## 10. 실행·행 상태

실행 상태 전이는 다음과 같습니다.

```text
RUNNING → COMPLETED
       └→ FAILED
       └→ INTERRUPTED → RUNNING
```

| 실행 상태 | 의미 |
| --- | --- |
| `RUNNING` | 행 검증을 마치고 실패 결과 저장 또는 실제 데이터 반영 중 |
| `COMPLETED` | 모든 행과 실행 완료 상태가 함께 커밋됨 |
| `FAILED` | 검증 또는 DB 반영 실패가 행 결과와 함께 저장됨 |
| `INTERRUPTED` | 재시작 시 미완료 `RUNNING`을 감지해 재시도 가능하게 변경 |

| 행 상태 | 의미 |
| --- | --- |
| `SUCCEEDED` | 전체 트랜잭션이 성공해 실제로 반영됨 |
| `FAILED` | 검증 또는 DB 반영의 직접 원인 행 |
| `SKIPPED` | 다른 행 오류 때문에 반영하지 않았거나 전체 롤백됨 |

반영 실행은 `GET /api/v1/planning-data/imports/{importRunId}`로 다시 조회할 수 있습니다.

## 11. 반영 오류 코드

| code | 의미 |
| --- | --- |
| `FILE_VALIDATION_FAILED` | 파일 안의 다른 행에 검증 오류가 있어 유효 행도 반영하지 않음 |
| `DB_APPLY_FAILED` | 미리보기 이후 동시 변경 또는 DB 제약으로 해당 행 반영 실패 |
| `TRANSACTION_ROLLED_BACK` | 다른 행의 DB 실패로 함께 롤백됨 |

실행 조회 실패는 `PLANNING_DATA_IMPORT_NOT_FOUND`, 같은 요청 키에 다른 파일을 사용한 충돌은
`PLANNING_DATA_IMPORT_REQUEST_CONFLICT`입니다.

## 12. 재시작 복구

애플리케이션 시작 완료 이벤트에서 DB에 남은 `RUNNING` 실행을 `INTERRUPTED`로 바꿉니다.
데이터 반영과 `COMPLETED` 저장이 한 트랜잭션이므로, 프로세스가 중간에 종료되면 계획 데이터는
커밋되지 않고 실행 시작 이력만 남습니다. 사용자가 같은 키와 같은 파일을 다시 보내면
`retryCount`를 올리고 CSV 검증부터 다시 수행합니다.

현재 복구는 단일 애플리케이션 인스턴스를 전제로 합니다. 외부 메시지 브로커, 분산 잠금,
여러 노드의 작업 소유권 이전은 범위에 포함하지 않습니다.

## 13. 검증

PostgreSQL Testcontainers 통합 테스트가 다음 경계를 실제 Flyway 스키마에서 확인합니다.

- 같은 요청을 두 번 보내도 도메인 데이터와 실행 이력이 한 번만 생성됨
- 같은 요청 키의 다른 파일은 충돌함
- 앞선 유효 행 뒤에 참조 오류가 있어도 모든 계획 테이블이 변경되지 않음
- 미리보기와 반영 사이의 DB 유일 제약 위반이 전체 롤백되고 행 오류로 남음
- `INTERRUPTED` 실행이 같은 파일로 완료되고 `retryCount`가 증가함
- 최대 2,000행이 한 실행으로 반영됨
