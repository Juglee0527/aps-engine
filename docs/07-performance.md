# Scheduling Performance Baseline

## 1. 목적

`ForwardScheduler`의 현재 성능을 반복 가능한 입력으로 관찰하고, 이후 최적화가 실제 효과를 내는지
비교할 기준선을 제공합니다. 이 단계에서는 알고리즘을 변경하지 않습니다.

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

## 6. 추가 관찰 방법

변경 전후 차이가 불명확하면 다음 순서로 근거를 보강합니다.

1. `performanceTest`를 같은 환경에서 여러 번 실행해 중앙값 비교
2. Gradle Test worker에 Java Flight Recorder를 연결해 allocation과 hot method 확인
3. 대량 시나리오의 task 수와 결과 동일성 확인
4. 확인된 단일 병목만 로드맵 `039`에서 개선
