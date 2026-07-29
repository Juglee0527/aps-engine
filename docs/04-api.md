# API 계약

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
| `INTERNAL_ERROR` | 500 | 사전에 정의하지 못한 서버 내부 오류 |

도메인별 오류 코드가 실제로 필요해지면 해당 기능을 구현하는 커밋에서 추가합니다. 현재 사용되지 않는 오류 코드를 미리 만들지 않습니다.

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
