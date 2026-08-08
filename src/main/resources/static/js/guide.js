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
        const observations = scenario.observationPoints
            .map((point) => `<li>${escapeHtml(point)}</li>`)
            .join("");
        return `
            <article class="guide-scenario-card">
                <span class="guide-course-index">LAB ${String(index + 1).padStart(2, "0")} · ${escapeHtml(scenario.key)}</span>
                <strong>${escapeHtml(scenario.title)}</strong>
                <p>${escapeHtml(scenario.objective)}</p>
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
                >${running ? "데이터 생성 및 계산 중…" : "이 실습 생성하고 실행"}</button>
            </article>
        `;
    }).join("");
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
