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

