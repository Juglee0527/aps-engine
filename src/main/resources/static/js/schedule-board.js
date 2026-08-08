import {state} from "./state.js";
import {
    escapeHtml,
    formatAxisTime,
    formatDateTime,
    formatTime,
    groupBy,
    number,
    text
} from "./ui.js";

const COLORS = ["#3f72d8", "#8b67e8", "#16a2b6", "#e37e35", "#397e69", "#c25477"];

export function renderScheduleBoard() {
    renderRunSummary();
    renderReadiness();
    renderMetrics();
    renderGantt();
    renderLoadRanking();
}

function renderRunSummary() {
    const schedule = state.latestSchedule;
    text("#run-title", schedule
        ? `RUN #${schedule.id} · ${schedule.orderCount}개 오더 계획 완료`
        : "아직 실행된 스케줄이 없습니다");
    text("#run-description", schedule
        ? "계획 결과의 규칙, 기간, 지연과 가동률을 기준으로 다음 의사결정을 검토하세요."
        : "계획을 실행하면 선택 규칙과 핵심 성과를 이곳에서 비교할 수 있습니다.");
    text("#planning-start", schedule ? formatDateTime(schedule.planningStart) : "-");
    text("#schedule-end", schedule ? formatDateTime(schedule.schedulingEnd) : "-");
    text("#source-run", schedule?.sourceScheduleRunId
        ? `RUN #${schedule.sourceScheduleRunId}` : "-");
    text("#frozen-at", schedule?.frozenAt ? formatDateTime(schedule.frozenAt) : "-");
    text("#dispatching-rule", schedule?.dispatchingRule || "-");
    text("#total-tardiness", schedule ? `${number(schedule.totalTardinessMinutes)}분` : "-");
    text("#makespan", schedule ? `${number(schedule.makespanMinutes)}분` : "-");
    text("#plan-utilization", schedule ? `${schedule.machineUtilizationPercent}%` : "-");
    const status = document.querySelector("#run-status");
    status.textContent = schedule?.status || "READY";
    status.className = `status-pill ${schedule ? "completed" : "neutral"}`;
    document.querySelector("#reschedule-button").disabled = !schedule;
}

function renderReadiness() {
    const readiness = document.querySelector("#schedule-readiness");
    const masterReady = state.factories.length > 0
        && state.machines.length > 0
        && state.routings.length > 0;
    const confirmed = state.orders.filter(
        (order) => order.status === "CONFIRMED"
    ).length;
    readiness.hidden = Boolean(state.latestSchedule);
    setReadinessStep("#readiness-master", masterReady, masterReady
        ? "기준정보 준비 완료" : "공장·설비·품목·Routing 필요");
    setReadinessStep("#readiness-orders", confirmed > 0, confirmed > 0
        ? `${confirmed}개 확정 오더 준비` : "확정 생산오더 필요");
}

function setReadinessStep(selector, ready, message) {
    const step = document.querySelector(selector);
    step.classList.toggle("is-ready", ready);
    step.querySelector("small").textContent = message;
}

function renderMetrics() {
    const confirmed = state.orders.filter((order) => order.status === "CONFIRMED").length;
    text("#confirmed-count", confirmed);
    text("#task-count", state.latestSchedule?.taskCount || 0);
    const delayed = state.latestSchedule?.delayedOrderCount || 0;
    text("#delayed-count", delayed);
    document.querySelector("#delayed-kpi").classList.toggle(
        "is-alert",
        delayed > 0
    );
    const candidate = state.bottleneckAnalysis?.candidates?.[0];
    const utilization = candidate?.utilizationPercent == null
        ? (candidate ? "CAPA 없음" : "0%")
        : `${candidate.utilizationPercent}%`;
    text("#peak-load", utilization);
    text("#peak-machine", candidate
        ? `${candidate.machineCode} · 병목 후보 #${candidate.rank}`
        : (state.latestSchedule ? "진단 후보 없음" : "계산 대기"));
}

function renderGantt() {
    const container = document.querySelector("#gantt");
    const legend = document.querySelector("#gantt-legend");
    container.replaceChildren();
    legend.replaceChildren();
    const schedule = state.latestSchedule;
    if (!schedule || schedule.tasks.length === 0) {
        container.innerHTML = `
            <div class="gantt-empty">
                <div><strong>표시할 스케줄이 없습니다</strong>
                <p>생산오더를 확정하고 오른쪽 위의 스케줄 실행 버튼을 눌러 주세요.</p></div>
            </div>`;
        return;
    }

    const start = state.scheduleTaskFilters.from
        ? new Date(state.scheduleTaskFilters.from).getTime()
        : new Date(schedule.planningStart).getTime();
    const requestedEnd = state.scheduleTaskFilters.to
        ? new Date(state.scheduleTaskFilters.to).getTime()
        : new Date(schedule.schedulingEnd).getTime();
    const end = Math.max(requestedEnd, start + 3600000);
    const duration = end - start;
    const chart = document.createElement("div");
    chart.className = "gantt-chart";
    chart.append(createGanttHeader(start, duration));

    const tasksByMachine = groupBy(schedule.tasks, (task) => task.machineId);
    for (const [machineId, tasks] of tasksByMachine) {
        const row = document.createElement("div");
        row.className = "gantt-row";
        const first = tasks[0];
        const label = document.createElement("div");
        label.className = "machine-label";
        label.innerHTML = `<strong>${escapeHtml(first.machineName)}</strong>
            <span>${escapeHtml(first.machineCode)} · ${tasks.length} TASKS</span>`;
        const timeline = document.createElement("div");
        timeline.className = "timeline";
        for (const task of tasks) {
            appendChangeover(timeline, task, start, duration);
            const bar = document.createElement("div");
            const taskStart = new Date(task.startAt).getTime();
            const taskEnd = new Date(task.endAt).getTime();
            bar.className = `gantt-bar${task.delayed ? " is-delayed" : ""}`;
            bar.style.left = `${Math.max(0, (taskStart - start) / duration * 100)}%`;
            bar.style.width = `${Math.max(1.2, (taskEnd - taskStart) / duration * 100)}%`;
            bar.style.background = colorFor(task.productionOrderId);
            bar.title = `${task.orderNumber} / ${task.operationName}\n${formatDateTime(task.startAt)} → ${formatDateTime(task.endAt)}\n작업 ${task.workingMinutes}분`;
            bar.innerHTML = `<strong>${escapeHtml(task.orderNumber)} · ${escapeHtml(task.operationCode)}</strong>
                <span>${formatTime(task.startAt)}–${formatTime(task.endAt)} · ${task.workingMinutes}m</span>`;
            timeline.append(bar);
        }
        row.append(label, timeline);
        chart.append(row);
    }
    container.append(chart);

    const distinctOrders = new Map();
    for (const task of schedule.tasks) {
        distinctOrders.set(task.productionOrderId, task.orderNumber);
    }
    for (const [orderId, orderNumber] of distinctOrders) {
        const item = document.createElement("span");
        item.className = "legend-item";
        item.innerHTML = `<span class="legend-color" style="background:${colorFor(orderId)}"></span>${escapeHtml(orderNumber)}`;
        legend.append(item);
    }
    const changeoverLegend = document.createElement("span");
    changeoverLegend.className = "legend-item";
    changeoverLegend.innerHTML = '<i class="legend-color changeover-color"></i>Changeover';
    legend.append(changeoverLegend);
}

function appendChangeover(timeline, task, start, duration) {
    if (!task.changeoverStartAt || task.changeoverMinutes <= 0) return;
    const bar = document.createElement("div");
    const changeoverStart = new Date(task.changeoverStartAt).getTime();
    const operationStart = new Date(task.startAt).getTime();
    bar.className = "gantt-bar gantt-changeover";
    bar.style.left = `${Math.max(0, (changeoverStart - start) / duration * 100)}%`;
    bar.style.width = `${Math.max(1.2, (operationStart - changeoverStart) / duration * 100)}%`;
    bar.title = `${task.orderNumber} / Changeover\n${formatDateTime(task.changeoverStartAt)} → ${formatDateTime(task.startAt)}\n준비작업 ${task.changeoverMinutes}분`;
    bar.innerHTML = `<strong>CHANGEOVER</strong><span>${task.changeoverMinutes}m</span>`;
    timeline.append(bar);
}

function createGanttHeader(start, duration) {
    const header = document.createElement("div");
    header.className = "gantt-header";
    const label = document.createElement("div");
    label.className = "machine-heading";
    label.textContent = "MACHINE / TIMELINE";
    const timeline = document.createElement("div");
    timeline.className = "timeline-header";
    for (let index = 0; index < 5; index++) {
        const tick = document.createElement("span");
        tick.className = "time-tick";
        tick.style.left = `${index * 20}%`;
        tick.textContent = formatAxisTime(new Date(start + duration * index / 5));
        timeline.append(tick);
    }
    header.append(label, timeline);
    return header;
}

function renderLoadRanking() {
    const container = document.querySelector("#load-ranking");
    container.replaceChildren();
    const candidates = new Map(
        (state.bottleneckAnalysis?.candidates || [])
            .map((candidate) => [candidate.machineId, candidate])
    );
    const loads = [...state.capacity.entries()].sort((left, right) => {
        const leftRank = candidates.get(left[0])?.rank ?? Number.MAX_SAFE_INTEGER;
        const rightRank = candidates.get(right[0])?.rank ?? Number.MAX_SAFE_INTEGER;
        return leftRank - rightRank || right[1].utilization - left[1].utilization;
    });
    if (loads.length === 0) {
        container.innerHTML = `<div class="load-empty">스케줄 실행 후 설비별<br>CAPA 사용률을 계산합니다.</div>`;
        return;
    }
    for (const [machineId, load] of loads) {
        const machine = state.machines.find((item) => item.id === machineId);
        const candidate = candidates.get(machineId);
        const utilization = candidate?.utilizationPercent ?? load.utilization;
        const utilizationLabel = candidate && candidate.utilizationPercent == null
            ? "CAPA 없음" : `${utilization}%`;
        const diagnosis = candidate
            ? `병목 #${candidate.rank} · ${bottleneckReason(candidate.reason)}`
            : "진단 임계치 미만";
        const item = document.createElement("div");
        item.className = "load-item";
        item.innerHTML = `
            <div class="load-item-top"><span>${escapeHtml(machine?.code || `MACHINE #${machineId}`)}</span><strong>${utilizationLabel}</strong></div>
            <div class="load-track"><div class="load-fill" style="width:${Math.min(100, utilization || 0)}%"></div></div>
            <div class="load-item-top"><span>LOAD ${number(load.workingMinutes)}m</span><span>CAPA ${number(load.availableMinutes)}m</span></div>
            <div class="load-item-top"><span>${diagnosis}</span></div>`;
        container.append(item);
    }
}

function bottleneckReason(reason) {
    return {
        NO_AVAILABLE_CAPACITY: "가용 CAPA 없음",
        CAPACITY_EXCEEDED: "CAPA 초과",
        HIGH_UTILIZATION: "사용률 80% 이상"
    }[reason] || reason;
}

function colorFor(id) {
    return COLORS[Math.abs(Number(id)) % COLORS.length];
}
