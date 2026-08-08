# Scheduling Performance

## 1. 목적

`ForwardScheduler`의 현재 성능을 반복 가능한 입력으로 관찰하고, 이후 최적화가 실제 효과를 내는지
비교할 기준선을 제공합니다. 기준선 측정 뒤에는 같은 입력과 환경에서 병목 하나만 개선하고
동작 동일성과 성능 변화를 함께 검증합니다.

## 2. 실행 분리

일반 테스트는 `performance` tag를 제외합니다.

```powershell
.\gradlew.bat test --no-daemon
```

성능 기준선만 실행하려면 별도 task를 사용합니다.

```powershell
.\gradlew.bat performanceTest --no-daemon
```

`performanceTest`는 항상 다시 실행되며 측정값을 표준 출력과 Gradle HTML/XML 결과에 남깁니다.
GitHub Actions 기본 Build workflow에는 포함하지 않습니다.

## 3. 입력 조건

모든 시나리오는 같은 조건으로 생성합니다.

- 월요일 08:00 계획 시작
- 월~금 08:00~18:00 근무
- 오더당 수량 1
- 공정당 가공시간 5분
- 납기는 계획 시작 후 365일
- Changeover와 Maintenance 없음
- 오더 우선순위와 품목은 100개 값으로 순환
- 공정은 설비에 균등 분산

| 시나리오 | 오더 | 오더당 공정 | 설비 | 결과 작업 |
| --- | ---: | ---: | ---: | ---: |
| small | 100 | 3 | 20 | 300 |
| medium | 1,000 | 5 | 50 | 5,000 |
| large | 5,000 | 5 | 100 | 25,000 |

측정 전 20개 오더·3개 공정 입력을 한 번 실행해 클래스 로딩과 기본 JIT warm-up 영향을 줄입니다.

## 4. 기준선 결과

측정 환경:

- 측정일: 2026-07-30
- OS: Windows
- Java: JDK 21
- Gradle: 8.14.4
- 실행: 단일 Gradle Test worker

| 시나리오 | 경과시간 | 실행 중 peak heap 증가 |
| --- | ---: | ---: |
| small | 6.320 ms | 0.000 MiB |
| medium | 35.540 ms | 6.000 MiB |
| large | 85.175 ms | 7.570 MiB |

`peakHeapDeltaMiB`는 시나리오 직전에 heap pool peak를 초기화하고, 실행 전 사용량과 실행 중 peak
사용량의 차이를 합산한 관찰값입니다. small의 0은 메모리 pool 측정 해상도와 기존 할당 공간 재사용
영향이며 “할당이 전혀 없음”을 의미하지 않습니다.

## 5. 해석 범위

- 이 테스트는 회귀 비교용 micro baseline이며 JMH benchmark가 아닙니다.
- 단일 실행값은 OS 부하, JIT와 GC 상태에 따라 달라질 수 있습니다.
- 절대 시간을 합격 기준으로 사용하지 않고 동일 환경의 변경 전후 비교에 사용합니다.
- DB 조회와 JPA 저장시간은 포함하지 않고 순수 스케줄 계산만 측정합니다.
- 현재 결과만으로 특정 코드가 병목이라고 단정하거나 최적화하지 않습니다.

## 6. JFR 프로파일링

Gradle Test worker에 Java Flight Recorder를 연결하려면 다음 명령을 사용합니다.

```powershell
.\gradlew.bat performanceTest --no-daemon -PperformanceJfr
```

이 명령은 표준 시나리오를 실행한 뒤 프로파일링 표본 확보용으로 100,000개 작업 시나리오를
10회 추가 실행하고, 다음 파일을 생성합니다.

```text
build/reports/performance/forward-scheduler.jfr
```

JDK 21의 `jfr` 명령으로 CPU와 할당 지점을 확인할 수 있습니다.

```powershell
jfr view hot-methods build/reports/performance/forward-scheduler.jfr
jfr view allocation-by-site build/reports/performance/forward-scheduler.jfr
```

`performance.profile` 시스템 속성은 `-PperformanceJfr` 사용 시에만 활성화됩니다. 따라서 일반
`performanceTest`의 입력과 측정 횟수에는 영향을 주지 않습니다.

## 7. 039 측정 기반 개선

### 7.1 확인한 병목

변경 전 JFR의 할당 지점에서 다음 항목을 확인했습니다.

| 할당 지점 | 할당 압력 |
| --- | ---: |
| `ForwardScheduler.schedule` | 16.80% |
| `OffsetDateTime.of` | 13.50% |
| `StreamSupport.stream` | 7.59% |
| `SortedOps.makeRef` | 6.71% |
| `WorkingTimeCalculator.allocate` | 6.00% |

성능 시나리오는 Changeover와 Maintenance가 없으므로 매 작업일마다 비가용 구간이 빈 목록입니다.
그런데 `WorkingTimeCalculator.subtractUnavailableIntervals()`는 빈 목록에도 stream과 정렬 파이프라인을
만들고, 이미 시간순인 가용 구간 결과를 다시 정렬했습니다. 이 공통 경로의 불필요한 할당만 제거하고
날짜 계산이나 스케줄 우선순위 규칙은 변경하지 않았습니다.

### 7.2 변경 내용과 복잡도

비가용 구간이 비어 있으면 이미 시간순으로 생성된 근무 구간을 불변 복사해 즉시 반환합니다.

- 변경 전: 비가용 구간 정규화 `O(u log u)`와 결과 재정렬 `O(w log w)`
- 변경 후 빈 구간: 근무 구간 불변 복사 `O(w)`
- 변경 후 비어 있지 않은 구간: 기존 차감·정렬 로직과 복잡도 유지

여기서 `u`는 비가용 구간 수, `w`는 조회 범위의 근무 구간 수입니다. 전체 스케줄러의 점근적
복잡도는 바뀌지 않으며, 실제로 반복되는 빈 구간 경로의 stream·정렬 객체 생성만 줄였습니다.

변경 후 JFR 할당 상위 항목에서는 `StreamSupport.stream`과 `SortedOps.makeRef`가 사라졌습니다.
JFR 표본 비율은 실행마다 달라질 수 있으므로 절대 할당량 감소로 해석하지 않고, 제거한 코드 경로가
더 이상 관찰되지 않는다는 근거로만 사용합니다.

### 7.3 변경 전후 결과

같은 Windows, JDK 21, Gradle 8.14.4 환경에서 JFR을 끈 표준 `performanceTest` 결과입니다.

| 시나리오 | 변경 전 | 변경 후 | 경과시간 감소 |
| --- | ---: | ---: | ---: |
| small | 6.320 ms | 3.543 ms | 43.9% |
| medium | 35.540 ms | 16.143 ms | 54.6% |
| large | 85.175 ms | 58.638 ms | 31.2% |

| 시나리오 | 변경 전 peak heap 증가 | 변경 후 peak heap 증가 |
| --- | ---: | ---: |
| small | 0.000 MiB | 0.000 MiB |
| medium | 6.000 MiB | 4.000 MiB |
| large | 7.570 MiB | 8.916 MiB |

경과시간은 세 입력 모두 감소했습니다. peak heap은 GC, pool 재사용과 측정 해상도 영향을 받고
large에서는 오히려 증가했으므로 메모리 개선을 주장하지 않습니다.

### 7.4 동작 동일성

- 빈 비가용 구간을 전달한 결과가 기존 overload 결과와 같은지 검증합니다.
- 빠른 경로의 반환 목록이 기존 계약대로 불변인지 검증합니다.
- 기존 Maintenance, Changeover와 전체 스케줄링 테스트로 비어 있지 않은 경로의 회귀를 확인합니다.

## 8. 추가 관찰 방법

변경 전후 차이가 불명확하면 다음 순서로 근거를 보강합니다.

1. `performanceTest`를 같은 환경에서 여러 번 실행해 중앙값 비교
2. Gradle Test worker에 Java Flight Recorder를 연결해 allocation과 hot method 확인
3. 대량 시나리오의 task 수와 결과 동일성 확인
4. 확인된 단일 병목만 다음 성능 개선 단위에서 변경

## 9. APS 학습 데이터 성능 기준

`LearningScenarioSchedulingTest`의 `PERFORMANCE` 시나리오는 20설비·600오더·600작업을
결정론적으로 계산하고 5초 안에 끝나는지 확인합니다. 이 테스트도 `performance` 태그를 사용하므로
일반 `test`에는 포함되지 않고 `performanceTest`에서 기존 small·medium·large 기준과 함께 실행됩니다.

2026년 8월 8일 Windows/JDK 21 실행 결과는 성능 테스트 2개 실패 0개입니다. 기존 엔진 기준의
측정 출력은 small 1.377ms, medium 15.067ms, large 54.606ms였습니다. 학습 시나리오의 5초 제한은
카탈로그 생성과 순수 스케줄 계산 회귀를 빠르게 감지하기 위한 넉넉한 상한이며 DB 저장·HTTP·DOM
렌더링 시간을 포함하지 않고 운영 SLA로 사용하지 않습니다.
