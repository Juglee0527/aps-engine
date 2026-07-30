# 계획 데이터 CSV 입력

## 1. 현재 범위

현재 구현은 CSV를 읽어 검증 결과를 미리 보여주는 단계까지입니다.

```text
UTF-8 CSV
  → 파일·헤더 검사
    → 코드 정규화와 행 타입별 값 검사
      → 앞선 유효 행·기존 DB 참조 확인
        → 성공·실패 예상 건수와 행별 오류 반환
```

미리보기는 `@Transactional(readOnly = true)`로 실행하며 Repository `save`를 호출하지 않습니다.
실제 반영, 요청 멱등성, 중단 복구는 다음 개발 단위의 책임입니다.

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

## 7. 오류 계약

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
