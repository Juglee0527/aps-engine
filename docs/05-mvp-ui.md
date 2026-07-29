# APS Operations MVP

## 1. 목적

현재 구현된 생산 자원 API를 개발 도구 없이 브라우저에서 확인하기 위한 첫 운영 화면입니다.
아직 구현되지 않은 Product, CAPA, Scheduling 기능을 화면에서 임의로 표현하지 않고 실제 API가 존재하는
Factory, ProductionLine, Machine 범위만 제공합니다.

## 2. 기술 선택

MVP 화면은 Spring Boot의 정적 리소스 기능을 사용하며 HTML, CSS, Vanilla JavaScript로 구성합니다.

- 서버 실행 한 번으로 API와 화면을 함께 확인할 수 있습니다.
- Node.js, npm 및 별도 프론트엔드 빌드 파이프라인이 필요하지 않습니다.
- 현재 화면 규모에서는 프레임워크 상태 관리보다 명시적인 계층 선택 흐름이 단순합니다.
- 향후 화면과 상호작용이 충분히 커지면 React 등 별도 프론트엔드 전환을 검토합니다.

프론트엔드 프레임워크 전환은 현재 요구사항이 아니므로 MVP에 포함하지 않습니다.

## 3. 화면 범위

```text
Factory 목록/등록
  → 선택 Factory의 ProductionLine 목록/등록
    → 선택 ProductionLine의 Machine 목록/등록
```

- 상단 지표는 현재 불러온 공장, 선택 공장의 생산라인, 선택 라인의 설비 수를 보여줍니다.
- 설비 상태는 `AVAILABLE`, `STOPPED`, `INACTIVE`로 구분합니다.
- 빈 목록과 상위 자원 미선택 상태를 서로 다른 안내로 표현합니다.
- API 오류는 화면 하단 알림으로 표시하고 서버 연결 상태를 상단에서 확인할 수 있습니다.

## 4. 데이터 흐름

화면은 동일 출처의 `/api/v1/**` API를 호출합니다. 별도 프록시나 CORS 설정은 필요하지 않습니다.
등록 성공 후 해당 계층을 다시 조회해 서버 데이터를 화면 상태의 기준으로 사용합니다.

최초 로딩과 선택 변경 시 최대 100건을 조회합니다. 이는 운영용 전체 목록 정책이 아니라 현재 MVP의
간단한 탐색 범위입니다. 실제 데이터가 100건을 넘는 단계에서는 검색과 페이지 이동 UI를 별도로 설계합니다.

## 5. 제외 범위

- 사용자 인증과 권한
- Factory, ProductionLine, Machine 수정 및 삭제
- Product, Routing, ProductionOrder 화면
- CAPA와 Scheduling 결과 시각화
- 서버 푸시 또는 실시간 자동 갱신

## 6. 실행 및 확인

PostgreSQL을 실행하고 로컬 프로필로 애플리케이션을 시작한 다음
`http://localhost:8080`에 접속합니다.

```powershell
docker compose up -d postgres
.\scripts\run-local.ps1
```

공장 등록 후 생산라인과 설비의 `+` 버튼이 순서대로 활성화되는지 확인합니다.
