import {SAMPLE_STEP_KEYS} from "./guide-data.js";
import {state} from "./state.js";
import {escapeHtml, text} from "./ui.js";

export function renderGuideStatus() {
    const availableMachines = state.machines.filter(
        (machine) => machine.status === "AVAILABLE"
    ).length;
    const confirmedOrders = state.orders.filter(
        (order) => order.status === "CONFIRMED"
    ).length;

    text(
        "#guide-facility-count",
        `${state.factories.length}개 공장 · ${state.lines.length}개 라인 · ${availableMachines}대 가용 설비`
    );
    text(
        "#guide-process-count",
        `${state.products.length}개 품목 · ${state.routings.length}개 Routing`
    );
    text("#guide-order-count", `확정 오더 ${confirmedOrders}건`);
    text(
        "#guide-run-count",
        state.latestSchedule
            ? `RUN #${state.latestSchedule.id} · 작업 ${state.latestSchedule.taskCount}건`
            : "실행 결과 없음"
    );
}

export function renderLearningScenarios() {
    const container = document.querySelector("#guide-scenario-grid");
    if (!container) return;
    container.innerHTML = state.learningScenarios.map((scenario, index) => {
        const running = state.runningLearningScenario === scenario.key;
        const progress = state.learningProgress.scenarios[scenario.key];
        const progressLabel = {
            STARTED: "준비됨",
            ANALYZED: "결과 확인",
            COMPLETED: "완료",
            NEEDS_REVIEW: "데이터 재확인"
        }[progress?.status] || "시작 전";
        const observations = scenario.observationPoints
            .map((point) => `<li>${escapeHtml(point)}</li>`)
            .join("");
        const estimatedMinutes = scenario.expectedOrderCount > 200
            ? 30
            : scenario.expectedOrderCount > 20 ? 20 : 10;
        return `
            <article class="guide-scenario-card">
                <span class="guide-course-index">LAB ${String(index + 1).padStart(2, "0")} · ${escapeHtml(scenario.key)}</span>
                <em class="guide-scenario-progress is-${escapeHtml((progress?.status || "NEW").toLowerCase())}">${escapeHtml(progressLabel)}</em>
                <strong>${escapeHtml(scenario.title)}</strong>
                <p>${escapeHtml(scenario.objective)}</p>
                <div class="guide-scenario-meta">
                    <span>COURSE ${escapeHtml(scenario.course)}</span>
                    <span>약 ${estimatedMinutes}분</span>
                    <span>${scenario.expectedOrderCount}개 오더</span>
                </div>
                <div class="guide-scenario-predict">
                    <b>실행 전 질문</b>
                    <span>${escapeHtml(scenario.predictionPrompt)}</span>
                </div>
                <ul>${observations}</ul>
                <small>${scenario.expectedMachineCount}대 설비 · ${scenario.expectedProductCount}개 품목 · ${scenario.expectedOrderCount}개 오더</small>
                <button
                    class="guide-step-action"
                    type="button"
                    data-learning-scenario="${escapeHtml(scenario.key)}"
                    ${state.runningLearningScenario ? "disabled" : ""}
                >${running ? "데이터 생성 및 비교 중…" : "이 실습으로 규칙 비교"}</button>
            </article>
        `;
    }).join("");
}

export function renderLearningProgress() {
    const container = document.querySelector("#guide-course-progress-grid");
    if (!container) return;
    const courses = ["A", "B", "C", "D", "E", "F"];
    let completed = 0;
    container.innerHTML = courses.map((course) => {
        const scenarios = state.learningScenarios.filter(
            (scenario) => scenario.course === course
        );
        const courseCompleted = scenarios.filter((scenario) =>
            state.learningProgress.scenarios[scenario.key]?.status === "COMPLETED"
        ).length;
        completed += courseCompleted;
        const percent = scenarios.length === 0
            ? 0
            : Math.round(courseCompleted / scenarios.length * 100);
        return `
            <article>
                <span>COURSE ${course}</span>
                <strong>${courseCompleted} / ${scenarios.length}</strong>
                <div><i style="width:${percent}%"></i></div>
            </article>
        `;
    }).join("");
    text(
        "#guide-overall-progress",
        `${completed} / ${state.learningScenarios.length}개 실습 완료`
    );
}

export function renderLearningCoach() {
    const section = document.querySelector("#guide-result-coach");
    const coach = state.learningCoach;
    section.hidden = !coach;
    if (!coach) return;
    text("#guide-coach-concept", coach.concept);
    text("#guide-coach-explanation", actualResultExplanation(coach));
    text("#guide-coach-next", coach.nextExperiment);
    document.querySelector("#guide-coach-questions").innerHTML =
        coach.observationQuestions.map((question) =>
            `<li>${escapeHtml(question)}</li>`
        ).join("");
    document.querySelector("#guide-coach-kpis").innerHTML =
        Object.entries(coach.kpiMeanings).map(([key, meaning]) => `
            <div><dt>${escapeHtml(key)}</dt><dd>${escapeHtml(meaning)}</dd></div>
        `).join("");
}

function actualResultExplanation(coach) {
    if (state.frozenHorizonLab) {
        const counts = state.frozenHorizonLab.changes.reduce((result, change) => {
            result[change.classification] = (result[change.classification] || 0) + 1;
            return result;
        }, {});
        return `${coach.resultExplanation} 실제 결과는 고정 ${counts.FIXED || 0}건, 이동 ${counts.MOVED || 0}건, 제외 ${counts.EXCLUDED || 0}건, 신규 ${counts.NEW || 0}건입니다.`;
    }
    if (state.constraintImpact) {
        const before = state.constraintImpact.withoutConstraint.makespanMinutes;
        const after = state.constraintImpact.withConstraint.makespanMinutes;
        return `${coach.resultExplanation} 제약 적용으로 Makespan이 ${after - before >= 0 ? "+" : ""}${after - before}분 변했습니다.`;
    }
    if (state.learningComparison) {
        return `${coach.resultExplanation} 현재 KPI 기준 추천 규칙은 ${state.learningComparison.recommendedRule}입니다.`;
    }
    if (state.latestSchedule) {
        return `${coach.resultExplanation} RUN #${state.latestSchedule.id}에는 ${state.latestSchedule.orderCount}개 오더와 ${state.latestSchedule.taskCount}개 작업이 있습니다.`;
    }
    return coach.resultExplanation;
}

export function renderRuleComparison() {
    const section = document.querySelector("#guide-rule-comparison");
    const container = document.querySelector("#guide-rule-result-grid");
    const comparison = state.learningComparison;
    section.hidden = !comparison;
    if (!comparison) {
        container.innerHTML = "";
        return;
    }
    text("#guide-rule-reason", comparison.recommendationReason);
    container.innerHTML = comparison.results.map((result) => {
        const recommended = result.dispatchingRule === comparison.recommendedRule;
        return `
            <article class="guide-rule-card ${recommended ? "is-recommended" : ""}">
                <span>${recommended ? "RECOMMENDED" : "RULE"}</span>
                <strong>${escapeHtml(result.dispatchingRule)}</strong>
                <dl>
                    <div><dt>총 지연</dt><dd>${result.totalTardinessMinutes}분</dd></div>
                    <div><dt>지연 오더</dt><dd>${result.delayedOrderCount}건</dd></div>
                    <div><dt>Makespan</dt><dd>${result.makespanMinutes}분</dd></div>
                    <div><dt>가동률</dt><dd>${result.machineUtilizationPercent}%</dd></div>
                </dl>
                <p>${result.orderSequence.map(escapeHtml).join(" → ")}</p>
                <button class="guide-step-action" type="button" data-confirm-learning-rule="${escapeHtml(result.dispatchingRule)}">
                    이 규칙으로 확정 실행
                </button>
            </article>
        `;
    }).join("");
}

export function renderConstraintImpact() {
    const section = document.querySelector("#guide-constraint-impact");
    const container = document.querySelector("#guide-constraint-result-grid");
    const impact = state.constraintImpact;
    section.hidden = !impact;
    if (!impact) {
        container.innerHTML = "";
        return;
    }
    text("#guide-constraint-explanation", impact.explanation);
    const cards = [
        ["제약 제거 기준", impact.withoutConstraint],
        ["제약 적용 결과", impact.withConstraint]
    ];
    container.innerHTML = cards.map(([label, result], index) => {
        const changeover = result.tasks.reduce(
            (sum, task) => sum + (task.changeoverMinutes || 0),
            0
        );
        const machines = new Set(result.tasks.map((task) => task.machineId)).size;
        return `
            <article class="guide-impact-card ${index === 1 ? "is-applied" : ""}">
                <span>${escapeHtml(label)}</span>
                <strong>${escapeHtml(impact.scenarioKey)}</strong>
                <dl>
                    <div><dt>Makespan</dt><dd>${result.makespanMinutes}분</dd></div>
                    <div><dt>총 지연</dt><dd>${result.totalTardinessMinutes}분</dd></div>
                    <div><dt>전환시간</dt><dd>${changeover}분</dd></div>
                    <div><dt>사용 설비</dt><dd>${machines}대</dd></div>
                </dl>
                <p>${result.orderSequence.map(escapeHtml).join(" → ")}</p>
            </article>
        `;
    }).join("");
}

export function renderFrozenHorizonLab() {
    const section = document.querySelector("#guide-frozen-horizon");
    const summary = document.querySelector("#guide-frozen-summary");
    const changes = document.querySelector("#guide-frozen-change-list");
    const lab = state.frozenHorizonLab;
    section.hidden = !lab;
    if (!lab) {
        summary.innerHTML = "";
        changes.innerHTML = "";
        return;
    }
    text("#guide-frozen-explanation", lab.explanation);
    text("#guide-frozen-at", formatLabTime(lab.frozenAt));
    text(
        "#guide-maintenance-window",
        `${formatLabTime(lab.maintenanceStartAt)} ~ ${formatLabTime(lab.maintenanceEndAt)}`
    );
    const cards = [
        ["기준 계획", lab.baseline],
        ["Frozen Horizon 재계획", lab.rescheduled]
    ];
    summary.innerHTML = cards.map(([label, run], index) => `
        <article class="guide-frozen-kpi ${index === 1 ? "is-rescheduled" : ""}">
            <span>${escapeHtml(label)} · RUN #${run.id}</span>
            <strong>${escapeHtml(run.dispatchingRule)}</strong>
            <dl>
                <div><dt>Makespan</dt><dd>${run.makespanMinutes}분</dd></div>
                <div><dt>총 지연</dt><dd>${run.totalTardinessMinutes}분</dd></div>
                <div><dt>지연 오더</dt><dd>${run.delayedOrderCount}건</dd></div>
                <div><dt>작업</dt><dd>${run.taskCount}건</dd></div>
            </dl>
        </article>
    `).join("");
    const labels = {
        FIXED: "고정",
        MOVED: "이동",
        EXCLUDED: "제외",
        NEW: "신규"
    };
    changes.innerHTML = lab.changes.map((change) => `
        <li class="is-${change.classification.toLowerCase()}">
            <span>${escapeHtml(labels[change.classification] || change.classification)}</span>
            <strong>${escapeHtml(change.orderNumber)}</strong>
            <small>${escapeHtml(change.reason)}</small>
            <code>${escapeHtml(formatChangeTime(change))}</code>
        </li>
    `).join("");
}

function formatChangeTime(change) {
    const before = change.beforeStartAt
        ? `${formatLabTime(change.beforeStartAt)}~${formatLabTime(change.beforeEndAt)}`
        : "없음";
    const after = change.afterStartAt
        ? `${formatLabTime(change.afterStartAt)}~${formatLabTime(change.afterEndAt)}`
        : "없음";
    return `${before} → ${after}`;
}

function formatLabTime(value) {
    if (!value) return "-";
    return new Date(value).toLocaleString("ko-KR", {
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        hour12: false
    });
}

export function renderSampleOnboarding({
    completion,
    runningSampleStep,
    guideLogMessage,
    setGuideLog
}) {
    const completedCount = SAMPLE_STEP_KEYS.filter((key) => completion[key]).length;
    const firstIncompleteIndex = SAMPLE_STEP_KEYS.findIndex((key) => !completion[key]);
    const currentIndex = firstIncompleteIndex === -1
        ? SAMPLE_STEP_KEYS.length - 1
        : firstIncompleteIndex;

    text("#guide-progress-text", `${completedCount} / ${SAMPLE_STEP_KEYS.length} 단계 완료`);
    const progressFill = document.querySelector("#guide-progress-fill");
    progressFill.style.width = `${completedCount / SAMPLE_STEP_KEYS.length * 100}%`;
    progressFill.parentElement.setAttribute("aria-valuenow", String(completedCount));

    if (runningSampleStep === null && guideLogMessage === null) {
        const nextStepNumber = completedCount === SAMPLE_STEP_KEYS.length
            ? null
            : completedCount + 1;
        setGuideLog(nextStepNumber === null
            ? "샘플 생산계획 준비가 끝났습니다. 스케줄 보드에서 결과를 확인해 주세요."
            : `${nextStepNumber}단계부터 이어서 진행할 수 있습니다.`);
    }

    SAMPLE_STEP_KEYS.forEach((key, index) => {
        const card = document.querySelector(`[data-guide-step-card="${key}"]`);
        const button = card.querySelector("[data-sample-step]");
        const status = document.querySelector(`#guide-step-${key}-status`);
        const isComplete = completion[key];
        const isRunning = runningSampleStep === key;
        const isLocked = !isComplete && index > currentIndex;
        const isCurrent = !isComplete && index === currentIndex;

        card.classList.toggle("is-complete", isComplete);
        card.classList.toggle("is-running", isRunning);
        card.classList.toggle("is-locked", isLocked);
        card.classList.toggle("is-current", isCurrent && !isRunning);

        if (isRunning) {
            status.textContent = "등록 중";
        } else if (isComplete) {
            status.textContent = "완료";
        } else if (isLocked) {
            status.textContent = "선행 단계 필요";
        } else {
            status.textContent = "다음 단계";
        }

        if (key === "schedule" && isComplete) {
            button.textContent = "스케줄 보드에서 결과 보기";
            button.disabled = runningSampleStep !== null;
        } else {
            button.textContent = {
                resources: "샘플 생산 자원 등록",
                process: "샘플 품목과 공정 등록",
                orders: "샘플 생산오더 등록",
                schedule: "샘플 스케줄 실행"
            }[key];
            button.disabled = isComplete || isLocked || runningSampleStep !== null;
        }
    });
}
