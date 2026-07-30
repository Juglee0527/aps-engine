# Docker Application Image

## 1. 이미지 구성

APS Engine 이미지는 빌드 도구를 런타임에 포함하지 않는 멀티 스테이지 구조입니다.

```text
gradle:8.14.4-jdk21-alpine
  → bootJar
    → eclipse-temurin:21-jre-alpine
      → 비루트 사용자 aps
        → aps-engine.jar
```

- 빌드 단계는 Java 21과 Gradle 8.14.4로 실행합니다.
- 런타임 단계에는 JRE와 애플리케이션 JAR만 복사합니다.
- 컨테이너 프로세스는 `root`가 아닌 `aps` 사용자로 실행합니다.
- JVM 옵션은 표준 `JAVA_TOOL_OPTIONS` 환경변수로 주입할 수 있습니다.

## 2. Health Check

Spring Boot Actuator의 다음 엔드포인트를 사용합니다.

```http
GET /actuator/health
```

Dockerfile healthcheck는 컨테이너 내부 `127.0.0.1:8080`으로 요청합니다. 데이터베이스 연결을 포함한
애플리케이션 상태가 준비되면 `healthy`가 됩니다.

## 3. Compose 실행

`.env`를 준비한 뒤 PostgreSQL과 애플리케이션을 함께 실행합니다.

```powershell
Copy-Item .env.example .env
docker compose up -d --build
docker compose ps
```

기본 접속 주소는 다음과 같습니다.

```text
애플리케이션: http://localhost:8080
Health:       http://localhost:8080/actuator/health
PostgreSQL:   localhost:5432
```

포트가 사용 중이면 `.env`에서 변경합니다.

```dotenv
APP_PORT=8081
POSTGRES_PORT=5433
```

컨테이너 내부에서 앱은 호스트 포트와 무관하게 `postgres:5432`, 앱 `8080`을 사용합니다.

## 4. 환경변수

| 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `POSTGRES_DB` | `aps` | 데이터베이스 이름 |
| `POSTGRES_USER` | `aps` | 데이터베이스 사용자 |
| `POSTGRES_PASSWORD` | 없음 | 필수 비밀번호 |
| `POSTGRES_PORT` | `5432` | 호스트 PostgreSQL 포트 |
| `APP_PORT` | `8080` | 호스트 애플리케이션 포트 |
| `JAVA_TOOL_OPTIONS` | 없음 | 선택 JVM 옵션 |

Compose는 앱에 `SPRING_PROFILES_ACTIVE=local`, `POSTGRES_HOST=postgres`와 컨테이너 내부 포트를
명시적으로 전달합니다. 비밀번호는 이미지에 포함하지 않습니다.

## 5. 종료

컨테이너만 종료하면 데이터 볼륨은 유지됩니다.

```powershell
docker compose down
```

`docker compose down -v`는 PostgreSQL 데이터를 삭제하므로 초기화가 명확히 필요한 경우에만
사용합니다.

## 6. 검증 결과

2026년 7월 30일 로컬 Docker Desktop에서 다음 항목을 확인했습니다.

- 멀티 스테이지 이미지 빌드 성공
- PostgreSQL 18.4와 Flyway migration 연결 성공
- 앱 컨테이너 사용자 `aps`
- PostgreSQL과 앱 컨테이너 `healthy`
- `/actuator/health` 응답 `UP`
- `/` 응답 HTTP 200
