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
