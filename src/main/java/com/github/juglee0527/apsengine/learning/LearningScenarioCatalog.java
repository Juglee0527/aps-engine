package com.github.juglee0527.apsengine.learning;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;
import com.github.juglee0527.apsengine.learning.LearningScenarioBlueprint.MachineSpec;
import com.github.juglee0527.apsengine.learning.LearningScenarioBlueprint.OperationSpec;
import com.github.juglee0527.apsengine.learning.LearningScenarioBlueprint.OrderSpec;
import com.github.juglee0527.apsengine.learning.LearningScenarioBlueprint.ProductSpec;

import org.springframework.stereotype.Component;

@Component
public class LearningScenarioCatalog {

    private final Map<String, LearningScenarioBlueprint> blueprints;

    public LearningScenarioCatalog() {
        Map<String, LearningScenarioBlueprint> values = new LinkedHashMap<>();
        register(values, firstPlan());
        register(values, finiteCapacity());
        register(values, precedence());
        register(values, tardiness());
        register(values, ruleComparison());
        register(values, changeover());
        register(values, maintenance());
        register(values, alternativeMachine());
        register(values, bottleneck());
        register(values, frozenHorizon());
        this.blueprints = Collections.unmodifiableMap(values);
    }

    public List<LearningScenarioDefinition> findAll() {
        return blueprints.values().stream()
                .map(LearningScenarioBlueprint::definition)
                .toList();
    }

    public LearningScenarioDefinition get(String key) {
        return blueprint(key).definition();
    }

    LearningScenarioBlueprint blueprint(String key) {
        LearningScenarioBlueprint blueprint = blueprints.get(normalize(key));
        if (blueprint == null) {
            throw new ApplicationException(
                    ErrorCode.LEARNING_SCENARIO_NOT_FOUND
            );
        }
        return blueprint;
    }

    private void register(
            Map<String, LearningScenarioBlueprint> values,
            LearningScenarioBlueprint blueprint
    ) {
        values.put(blueprint.key(), blueprint);
    }

    private String normalize(String key) {
        return key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
    }

    private LearningScenarioBlueprint firstPlan() {
        return new LearningScenarioBlueprint(
                "FIRST_PLAN",
                "A",
                "첫 생산계획",
                "두 품목의 공정과 오더를 한 장의 유한능력 계획으로 만듭니다.",
                "기준정보가 생산오더와 작업 일정으로 연결되는 흐름을 이해합니다.",
                "절단과 조립 설비에서 어떤 오더가 먼저 시작할지 예상해 보세요.",
                List.of("오더별 공정 순서", "설비별 작업 배치", "계획 시작과 종료"),
                "우선순위와 납기, 공정 순서, 설비 가용시간을 함께 적용한 결과입니다.",
                "PO-04의 우선순위를 100으로 바꾼 뒤 작업 순서를 비교해 보세요.",
                List.of(
                        new MachineSpec("CUT", "절단기"),
                        new MachineSpec("ASM", "조립기")
                ),
                List.of(
                        product("PANEL", "제어 패널", 45, 60),
                        product("BOX", "제어 박스", 30, 50)
                ),
                List.of(
                        order("PANEL", "PO-01", 2, 0, 600, 90),
                        order("BOX", "PO-02", 2, 0, 720, 80),
                        order("PANEL", "PO-03", 1, 60, 840, 70),
                        order("BOX", "PO-04", 1, 120, 960, 60)
                )
        );
    }

    private LearningScenarioBlueprint finiteCapacity() {
        return new LearningScenarioBlueprint(
                "FINITE_CAPACITY",
                "A",
                "유한 CAPA와 대기",
                "하나의 병목 설비에 여러 오더를 투입해 무한능력 계획과 차이를 봅니다.",
                "설비는 동시에 하나의 작업만 수행한다는 유한능력 제약을 이해합니다.",
                "각 6시간 작업 네 건이 하루 9시간 근무 안에서 어디까지 끝날지 계산해 보세요.",
                List.of("설비 작업 겹침 여부", "비근무시간 건너뛰기", "오더 대기시간"),
                "작업은 겹치지 않고 근무시간 안에서 직렬 배치되어 다음 근무일로 이어집니다.",
                "수량 하나를 절반으로 줄인 뒤 makespan과 대기시간 변화를 확인해 보세요.",
                List.of(new MachineSpec("BOT", "병목 가공기")),
                List.of(new ProductSpec(
                        "SHAFT",
                        "구동축",
                        List.of(new OperationSpec(
                                1, "TURN", "선삭", 180, "BOT"
                        ))
                )),
                List.of(
                        order("SHAFT", "FC-01", 2, 0, 1_440, 90),
                        order("SHAFT", "FC-02", 2, 0, 1_800, 80),
                        order("SHAFT", "FC-03", 2, 0, 2_160, 70),
                        order("SHAFT", "FC-04", 2, 0, 2_520, 60)
                )
        );
    }

    private LearningScenarioBlueprint precedence() {
        return new LearningScenarioBlueprint(
                "PRECEDENCE",
                "A",
                "공정 선후관계",
                "가공·검사·포장의 세 공정이 반드시 순서대로 이어지는 흐름을 관찰합니다.",
                "후속 공정은 선행 공정 완료 전 시작할 수 없음을 이해합니다.",
                "설비가 모두 비어 있어도 검사와 포장이 08시에 시작하지 않는 이유를 생각해 보세요.",
                List.of("공정 sequence", "선행 종료와 후속 시작", "오더 간 파이프라인"),
                "각 설비의 가용 여부뿐 아니라 같은 오더의 선행 공정 완료시각이 다음 시작을 제한합니다.",
                "검사시간을 120분으로 늘려 오더 간 파이프라인이 어떻게 바뀌는지 보세요.",
                List.of(
                        new MachineSpec("MAKE", "가공기"),
                        new MachineSpec("TEST", "검사기"),
                        new MachineSpec("PACK", "포장기")
                ),
                List.of(new ProductSpec(
                        "MODULE",
                        "센서 모듈",
                        List.of(
                                new OperationSpec(1, "MAKE", "가공", 90, "MAKE"),
                                new OperationSpec(2, "TEST", "검사", 60, "TEST"),
                                new OperationSpec(3, "PACK", "포장", 30, "PACK")
                        )
                )),
                List.of(
                        order("MODULE", "PR-01", 1, 0, 480, 90),
                        order("MODULE", "PR-02", 1, 0, 600, 80)
                )
        );
    }

    private LearningScenarioBlueprint tardiness() {
        return new LearningScenarioBlueprint(
                "TARDINESS",
                "A",
                "납기 지연 읽기",
                "의도적으로 빡빡한 납기를 둬 지연 오더와 총 지연시간 KPI를 만듭니다.",
                "CAPA보다 많은 부하가 납기 성과에 미치는 영향을 이해합니다.",
                "한 설비에 필요한 총 작업시간과 각 납기를 비교해 어떤 오더가 늦을지 예상해 보세요.",
                List.of("납기 초과 작업", "delayedOrderCount", "totalTardinessMinutes"),
                "가용 CAPA보다 납기 전 요구 작업량이 많아 우선순위가 낮은 오더부터 지연이 누적됩니다.",
                "EDD 규칙으로 실행해 명시적 우선순위 결과와 총 지연시간을 비교해 보세요.",
                List.of(new MachineSpec("WELD", "용접기")),
                List.of(
                        singleOperationProduct("FRAME-A", "프레임 A"),
                        singleOperationProduct("FRAME-B", "프레임 B")
                ),
                List.of(
                        order("FRAME-A", "TD-01", 2, 0, 240, 100),
                        order("FRAME-B", "TD-02", 2, 0, 480, 80),
                        order("FRAME-A", "TD-03", 2, 0, 720, 60),
                        order("FRAME-B", "TD-04", 2, 0, 960, 40)
                )
        );
    }

    private LearningScenarioBlueprint ruleComparison() {
        return new LearningScenarioBlueprint(
                "RULE_COMPARISON",
                "C",
                "Dispatching Rule 비교",
                "우선순위·납기·가공시간이 서로 다른 오더를 세 규칙으로 동시에 계산합니다.",
                "배차 규칙이 작업 순서와 KPI를 어떻게 바꾸는지 비교합니다.",
                "Priority, EDD, SPT가 각각 어떤 오더를 첫 작업으로 고를지 예상해 보세요.",
                List.of("규칙별 첫 오더", "총 지연시간", "Makespan과 가동률"),
                "같은 입력도 규칙의 첫 정렬 기준이 달라 작업 순서와 납기 성과가 달라집니다.",
                "추천 규칙과 다른 규칙을 확정 실행해 간트의 첫 작업을 확인해 보세요.",
                List.of(new MachineSpec("CELL", "공용 가공 셀")),
                List.of(
                        comparisonProduct("LONG", "장시간 부품", 180),
                        comparisonProduct("MEDIUM", "중간 부품", 90),
                        comparisonProduct("SHORT", "단시간 부품", 30)
                ),
                List.of(
                        order("LONG", "RC-LONG-1", 2, 0, 1_200, 100),
                        order("MEDIUM", "RC-MED-1", 1, 0, 240, 50),
                        order("SHORT", "RC-SHORT-1", 1, 0, 480, 20),
                        order("LONG", "RC-LONG-2", 1, 0, 1_440, 90),
                        order("MEDIUM", "RC-MED-2", 1, 0, 600, 40),
                        order("SHORT", "RC-SHORT-2", 1, 0, 720, 10)
                )
        );
    }

    private LearningScenarioBlueprint changeover() {
        return new LearningScenarioBlueprint(
                "CHANGEOVER",
                "D",
                "품목 전환시간",
                "같은 설비에서 A와 B 품목을 번갈아 만들며 방향별 준비시간을 관찰합니다.",
                "품목 순서가 순가공시간이 같아도 완료시각을 바꾸는 이유를 이해합니다.",
                "A→B 120분, B→A 15분일 때 어떤 전환이 간트에 길게 보일지 예상해 보세요.",
                List.of("전환 시작 구간", "방향별 changeoverMinutes", "Makespan 증가"),
                "이전 품목과 다음 품목 조합에 맞는 방향성 전환시간이 가공 전에 추가됩니다.",
                "우선순위를 바꿔 같은 품목을 묶었을 때 총 준비시간이 줄어드는지 확인해 보세요.",
                List.of(new MachineSpec("CELL", "공용 가공 셀")),
                List.of(
                        constraintProduct("ITEM-A", "품목 A", "CELL", 60),
                        constraintProduct("ITEM-B", "품목 B", "CELL", 60)
                ),
                List.of(
                        order("ITEM-A", "CO-A-1", 1, 0, 600, 100),
                        order("ITEM-B", "CO-B-1", 1, 0, 720, 90),
                        order("ITEM-A", "CO-A-2", 1, 0, 840, 80),
                        order("ITEM-B", "CO-B-2", 1, 0, 960, 70)
                )
        );
    }

    private LearningScenarioBlueprint maintenance() {
        return new LearningScenarioBlueprint(
                "MAINTENANCE",
                "D",
                "계획 정비 회피",
                "오전 10시부터 13시까지 정비가 잡힌 설비에 세 작업을 배치합니다.",
                "설비 정비가 가용시간을 차감하고 작업을 앞뒤 근무구간으로 나누는 방식을 이해합니다.",
                "08시에 시작한 작업들이 정비 구간을 어떻게 건너뛸지 예상해 보세요.",
                List.of("정비와 작업 비겹침", "작업 종료시각 이동", "가용 CAPA 감소"),
                "스케줄러는 정비 구간을 비가용시간으로 취급해 작업 시간을 정비 이후로 이어 붙입니다.",
                "정비 종료를 1시간 늦췄을 때 마지막 작업 완료시각 변화를 확인해 보세요.",
                List.of(new MachineSpec("PRESS", "프레스")),
                List.of(constraintProduct("PLATE", "프레스 판", "PRESS", 90)),
                List.of(
                        order("PLATE", "MT-01", 1, 0, 480, 100),
                        order("PLATE", "MT-02", 1, 0, 600, 90),
                        order("PLATE", "MT-03", 1, 0, 720, 80)
                )
        );
    }

    private LearningScenarioBlueprint alternativeMachine() {
        return new LearningScenarioBlueprint(
                "ALTERNATIVE_MACHINE",
                "D",
                "대체 설비 선택",
                "한 공정을 두 대의 후보 설비에서 처리해 가장 이른 완료 설비를 선택합니다.",
                "후보 우선순위보다 완료 가능시각이 먼저 평가되는 방식을 이해합니다.",
                "첫 작업이 기본 설비를 점유한 뒤 두 번째 작업이 어느 설비로 갈지 예상해 보세요.",
                List.of("작업별 machineId", "후보 설비 분산", "Makespan 단축"),
                "완료시각이 같으면 후보 우선순위를 따르지만, 대기 차이가 생기면 더 빨리 끝나는 설비를 고릅니다.",
                "대체 설비 근무 시작을 늦춰 선택이 다시 기본 설비로 돌아오는지 확인해 보세요.",
                List.of(
                        new MachineSpec("PRIMARY", "기본 가공기"),
                        new MachineSpec("ALT", "대체 가공기")
                ),
                List.of(new ProductSpec(
                        "GEAR",
                        "기어",
                        List.of(new OperationSpec(
                                1,
                                "MILL",
                                "밀링",
                                180,
                                "PRIMARY",
                                Map.of("PRIMARY", 1, "ALT", 2)
                        ))
                )),
                List.of(
                        order("GEAR", "AM-01", 1, 0, 600, 100),
                        order("GEAR", "AM-02", 1, 0, 720, 90),
                        order("GEAR", "AM-03", 1, 0, 840, 80)
                )
        );
    }

    private LearningScenarioBlueprint bottleneck() {
        return new LearningScenarioBlueprint(
                "BOTTLENECK",
                "D",
                "병목 공정 찾기",
                "30분 전처리와 후처리 사이에 180분 열처리를 둬 대기가 쌓이는 지점을 만듭니다.",
                "가장 느린 공정이 전체 흐름과 설비 가동률을 지배하는 현상을 이해합니다.",
                "세 설비 중 어느 설비 앞에 작업 대기가 누적될지 예상해 보세요.",
                List.of("설비별 작업분", "열처리 앞 대기", "최고 가동률"),
                "열처리의 처리시간이 전후 공정보다 길어 전체 생산율을 제한하고 병목 후보가 됩니다.",
                "열처리 설비를 한 대 더 후보로 추가했을 때 makespan 변화를 확인해 보세요.",
                List.of(
                        new MachineSpec("PREP", "전처리기"),
                        new MachineSpec("HEAT", "열처리기"),
                        new MachineSpec("FINISH", "후처리기")
                ),
                List.of(new ProductSpec(
                        "PART",
                        "열처리 부품",
                        List.of(
                                new OperationSpec(1, "PREP", "전처리", 30, "PREP"),
                                new OperationSpec(2, "HEAT", "열처리", 180, "HEAT"),
                                new OperationSpec(3, "FINISH", "후처리", 30, "FINISH")
                        )
                )),
                List.of(
                        order("PART", "BN-01", 1, 0, 720, 100),
                        order("PART", "BN-02", 1, 0, 900, 90),
                        order("PART", "BN-03", 1, 0, 1_080, 80),
                        order("PART", "BN-04", 1, 0, 1_260, 70)
                )
        );
    }

    private LearningScenarioBlueprint frozenHorizon() {
        return new LearningScenarioBlueprint(
                "FROZEN_HORIZON",
                "E",
                "긴급오더와 Frozen Horizon",
                "기준 계획에 긴급오더·정비·취소를 반영하되 이미 시작한 작업은 보호합니다.",
                "계획 안정성과 긴급 대응 사이에서 Frozen Horizon이 하는 역할을 이해합니다.",
                "10시 동결 경계를 걸친 작업과 그 뒤의 작업이 각각 어떻게 처리될지 예상해 보세요.",
                List.of("경계와 겹친 고정 작업", "이동·제외·신규 작업", "재계획 전후 KPI"),
                "경계 전에 시작한 작업은 끝날 때까지 고정되고, 이후 작업만 정비와 긴급오더를 반영해 다시 배치됩니다.",
                "동결 기준을 1시간 앞뒤로 옮겼을 때 고정되는 작업 수와 납기 성과를 비교해 보세요.",
                List.of(new MachineSpec("CELL", "긴급 대응 셀")),
                List.of(constraintProduct("MODULE", "긴급 대응 모듈", "CELL", 60)),
                List.of(
                        order("MODULE", "FH-KEEP", 3, 0, 420, 100),
                        order("MODULE", "FH-MOVE", 2, 0, 600, 80),
                        order("MODULE", "FH-CANCEL", 1, 0, 720, 60)
                )
        );
    }

    private ProductSpec product(
            String code,
            String name,
            int firstMinutes,
            int secondMinutes
    ) {
        return new ProductSpec(
                code,
                name,
                List.of(
                        new OperationSpec(1, "CUT", "절단", firstMinutes, "CUT"),
                        new OperationSpec(2, "ASM", "조립", secondMinutes, "ASM")
                )
        );
    }

    private ProductSpec singleOperationProduct(String code, String name) {
        return new ProductSpec(
                code,
                name,
                List.of(new OperationSpec(
                        1, "WELD", "용접", 180, "WELD"
                ))
        );
    }

    private ProductSpec comparisonProduct(
            String code,
            String name,
            int processingMinutes
    ) {
        return new ProductSpec(
                code,
                name,
                List.of(new OperationSpec(
                        1,
                        "PROCESS",
                        "가공",
                        processingMinutes,
                        "CELL"
                ))
        );
    }

    private ProductSpec constraintProduct(
            String code,
            String name,
            String machineCode,
            int processingMinutes
    ) {
        return new ProductSpec(
                code,
                name,
                List.of(new OperationSpec(
                        1,
                        "PROCESS",
                        "가공",
                        processingMinutes,
                        machineCode
                ))
        );
    }

    private OrderSpec order(
            String productCode,
            String orderNumber,
            long quantity,
            long releaseOffsetMinutes,
            long dueOffsetMinutes,
            int priority
    ) {
        return new OrderSpec(
                productCode,
                orderNumber,
                quantity,
                releaseOffsetMinutes,
                dueOffsetMinutes,
                priority
        );
    }
}
