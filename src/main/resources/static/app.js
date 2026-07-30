const API = {
    factories: "/api/v1/factories",
    lines: (factoryId) => `/api/v1/factories/${factoryId}/production-lines`,
    machines: (lineId) => `/api/v1/production-lines/${lineId}/machines`,
    products: "/api/v1/products",
    routings: (productId) => `/api/v1/products/${productId}/routings`,
    orders: "/api/v1/production-orders",
    confirmOrder: (orderId) => `/api/v1/production-orders/${orderId}/confirm`,
    calendars: (machineId) => `/api/v1/machines/${machineId}/working-calendars`,
    availability: (machineId, from, to) => {
        const query = new URLSearchParams({from, to});
        return `/api/v1/machines/${machineId}/availability?${query}`;
    },
    schedules: "/api/v1/schedules",
    latestSchedule: "/api/v1/schedules/latest",
    bottlenecks: (scheduleRunId) =>
        `/api/v1/schedules/${scheduleRunId}/bottlenecks`
};

const SAMPLE_DATA = {
    factory: {code: "DEMO-FACTORY", name: "데모 공장"},
    line: {code: "DEMO-LINE", name: "기본 생산라인"},
    machines: [
        {code: "DEMO-CUT", name: "데모 절단기", status: "AVAILABLE"},
        {code: "DEMO-ASSEMBLY", name: "데모 조립기", status: "AVAILABLE"}
    ],
    weekdays: ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
    product: {code: "DEMO-PRODUCT", name: "완제품 A", unit: "PIECE"},
    routing: {
        code: "DEMO-ROUTING",
        name: "표준 생산 Routing",
        operations: [
            {
                sequence: 10,
                code: "CUT",
                name: "절단",
                processingTimeMinutes: 15,
                machineCode: "DEMO-CUT"
            },
            {
                sequence: 20,
                code: "ASSEMBLY",
                name: "조립",
                processingTimeMinutes: 20,
                machineCode: "DEMO-ASSEMBLY"
            }
        ]
    },
    orders: [
        {orderNumber: "DEMO-ORDER-HIGH", quantity: 2, priority: 90, dueWorkingDays: 2},
        {orderNumber: "DEMO-ORDER-NORMAL", quantity: 3, priority: 60, dueWorkingDays: 3}
    ]
};

const SAMPLE_STEP_KEYS = ["resources", "process", "orders", "schedule"];

const state = {
    factories: [],
    lines: [],
    machines: [],
    products: [],
    routings: [],
    orders: [],
    latestSchedule: null,
    capacity: new Map(),
    bottleneckAnalysis: null,
    sampleCalendars: new Map()
};

const colors = ["#3f72d8", "#8b67e8", "#16a2b6", "#e37e35", "#397e69", "#c25477"];
let toastTimer;
let runningSampleStep = null;
let guideLogMessage = null;

document.addEventListener("DOMContentLoaded", async () => {
    bindNavigation();
    bindDialogs();
    bindForms();
    bindActions();
    setDefaultDates();
    await loadAll();
});

async function request(url, options = {}) {
    const {allowNotFound = false, ...fetchOptions} = options;
    const response = await fetch(url, {
        headers: {"Content-Type": "application/json", ...fetchOptions.headers},
        ...fetchOptions
    });
    if (allowNotFound && response.status === 404) {
        return null;
    }
    if (!response.ok) {
        const error = await response.json().catch(() => null);
        const message = error?.message
            || error?.detail
            || `요청에 실패했습니다. (${response.status})`;
        throw new Error(message);
    }
    return response.status === 204 ? null : response.json();
}

async function loadAll() {
    setConnection("checking");
    try {
        const [factoryPage, productPage, orderPage, latestSchedule] =
            await Promise.all([
                request(`${API.factories}?page=0&size=100`),
                request(`${API.products}?page=0&size=100`),
                request(`${API.orders}?page=0&size=100`),
                request(API.latestSchedule, {allowNotFound: true})
            ]);
        state.factories = factoryPage.content;
        state.products = productPage.content;
        state.orders = orderPage.content;
        state.latestSchedule = latestSchedule;

        const linePages = await Promise.all(
            state.factories.map((factory) =>
                request(`${API.lines(factory.id)}?page=0&size=100`)
            )
        );
        state.lines = linePages.flatMap((page) => page.content);

        const machinePages = await Promise.all(
            state.lines.map((line) =>
                request(`${API.machines(line.id)}?page=0&size=100`)
            )
        );
        state.machines = machinePages.flatMap((page) => page.content);

        const routingLists = await Promise.all(
            state.products.map((product) => request(API.routings(product.id)))
        );
        state.routings = routingLists.flat();
        await loadSampleCalendars();
        await Promise.all([loadCapacity(), loadBottlenecks()]);
        setConnection("online");
        render();
    } catch (error) {
        setConnection("offline");
        showToast(error.message, true);
        render();
    }
}

async function loadSampleCalendars() {
    state.sampleCalendars = new Map();
    const {machines} = sampleEntities();
    const existingMachines = machines.filter(Boolean);
    const calendarLists = await Promise.all(
        existingMachines.map((machine) => request(API.calendars(machine.id)))
    );
    existingMachines.forEach((machine, index) => {
        state.sampleCalendars.set(machine.id, calendarLists[index]);
    });
}

async function loadCapacity() {
    state.capacity = new Map();
    const schedule = state.latestSchedule;
    if (!schedule || schedule.tasks.length === 0) {
        return;
    }
    const machineIds = [...new Set(schedule.tasks.map((task) => task.machineId))];
    const results = await Promise.all(machineIds.map(async (machineId) => {
        const workingMinutes = schedule.tasks
            .filter((task) => task.machineId === machineId)
            .reduce(
                (sum, task) =>
                    sum + task.workingMinutes + (task.changeoverMinutes || 0),
                0
            );
        try {
            const availability = await request(API.availability(
                machineId,
                withOffset(
                    schedule.planningStart,
                    schedule.planningOffsetSeconds
                ),
                withOffset(
                    schedule.schedulingEnd,
                    schedule.planningOffsetSeconds
                )
            ));
            const utilization = availability.availableMinutes === 0
                ? 0
                : Math.round(workingMinutes / availability.availableMinutes * 100);
            return [machineId, {
                availableMinutes: availability.availableMinutes,
                workingMinutes,
                utilization
            }];
        } catch {
            return [machineId, {availableMinutes: 0, workingMinutes, utilization: 0}];
        }
    }));
    state.capacity = new Map(results);
}

async function loadBottlenecks() {
    state.bottleneckAnalysis = null;
    const schedule = state.latestSchedule;
    if (!schedule) {
        return;
    }
    state.bottleneckAnalysis = await request(
        API.bottlenecks(schedule.id)
    );
}

function render() {
    renderRunSummary();
    renderMetrics();
    renderGantt();
    renderOrderTables();
    renderLoadRanking();
    renderMasterData();
    renderGuide();
    renderSampleOnboarding();
    populateSelects();
}

function renderGuide() {
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

function renderSampleOnboarding() {
    const completion = sampleCompletion();
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

function renderRunSummary() {
    const schedule = state.latestSchedule;
    text("#run-title", schedule
        ? `RUN #${schedule.id} · ${schedule.orderCount}개 오더 계획 완료`
        : "아직 실행된 스케줄이 없습니다");
    text("#planning-start", schedule ? formatDateTime(schedule.planningStart) : "-");
    text("#schedule-end", schedule ? formatDateTime(schedule.schedulingEnd) : "-");
    const status = document.querySelector("#run-status");
    status.textContent = schedule?.status || "READY";
    status.className = `status-pill ${schedule ? "completed" : "neutral"}`;
}

function renderMetrics() {
    const confirmed = state.orders.filter((order) => order.status === "CONFIRMED").length;
    text("#confirmed-count", confirmed);
    text("#task-count", state.latestSchedule?.taskCount || 0);
    text("#delayed-count", state.latestSchedule?.delayedOrderCount || 0);
    const candidate = state.bottleneckAnalysis?.candidates?.[0];
    const utilization = candidate?.utilizationPercent == null
        ? (candidate ? "CAPA 없음" : "0%")
        : `${candidate.utilizationPercent}%`;
    text("#peak-load", utilization);
    text(
        "#peak-machine",
        candidate
            ? `${candidate.machineCode} · 병목 후보 #${candidate.rank}`
            : (state.latestSchedule ? "진단 후보 없음" : "계산 대기")
    );
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

    const start = new Date(schedule.planningStart).getTime();
    const end = Math.max(new Date(schedule.schedulingEnd).getTime(), start + 3600000);
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
            if (task.changeoverStartAt && task.changeoverMinutes > 0) {
                const changeoverBar = document.createElement("div");
                const changeoverStart =
                    new Date(task.changeoverStartAt).getTime();
                const operationStart = new Date(task.startAt).getTime();
                changeoverBar.className =
                    "gantt-bar gantt-changeover";
                changeoverBar.style.left =
                    `${Math.max(
                        0,
                        (changeoverStart - start) / duration * 100
                    )}%`;
                changeoverBar.style.width =
                    `${Math.max(
                        1.2,
                        (operationStart - changeoverStart)
                            / duration * 100
                    )}%`;
                changeoverBar.title =
                    `${task.orderNumber} / Changeover\n`
                    + `${formatDateTime(task.changeoverStartAt)}`
                    + ` → ${formatDateTime(task.startAt)}\n`
                    + `준비작업 ${task.changeoverMinutes}분`;
                changeoverBar.innerHTML =
                    `<strong>CHANGEOVER</strong>`
                    + `<span>${task.changeoverMinutes}m</span>`;
                timeline.append(changeoverBar);
            }
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
    changeoverLegend.innerHTML =
        '<i class="legend-color changeover-color"></i>Changeover';
    legend.append(changeoverLegend);
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

function renderOrderTables() {
    const summary = document.querySelector("#order-summary-body");
    const full = document.querySelector("#order-table-body");
    summary.replaceChildren();
    full.replaceChildren();
    if (state.orders.length === 0) {
        summary.innerHTML = `<tr><td colspan="5"><div class="table-empty">등록된 생산오더가 없습니다.</div></td></tr>`;
        full.innerHTML = `<tr><td colspan="8"><div class="table-empty">마스터 데이터를 구성한 뒤 첫 생산오더를 등록해 주세요.</div></td></tr>`;
        return;
    }
    const ordered = [...state.orders].sort(compareOrders);
    for (const order of ordered.slice(0, 6)) {
        const row = document.createElement("tr");
        row.innerHTML = `
            <td><strong>${escapeHtml(order.orderNumber)}</strong><small>ROUTING #${order.routingId}</small></td>
            <td>${number(order.quantity)}</td>
            <td>${formatDateTime(order.dueAt)}</td>
            <td><strong>P${order.priority}</strong></td>
            <td>${statusBadge(order.status)}</td>`;
        summary.append(row);
    }
    for (const order of ordered) {
        const routing = state.routings.find((item) => item.id === order.routingId);
        const product = state.products.find((item) => item.id === order.productId);
        const row = document.createElement("tr");
        row.innerHTML = `
            <td><strong>${escapeHtml(order.orderNumber)}</strong><small>#${order.id}</small></td>
            <td><strong>${escapeHtml(product?.name || "-")}</strong><small>${escapeHtml(routing?.code || "-")}</small></td>
            <td>${number(order.quantity)}</td>
            <td>${formatDateTime(order.releaseAt)}</td>
            <td>${formatDateTime(order.dueAt)}</td>
            <td><strong>P${order.priority}</strong></td>
            <td>${statusBadge(order.status)}</td>
            <td>${order.status === "DRAFT" ? `<button class="row-action" data-confirm-order="${order.id}">확정</button>` : ""}</td>`;
        full.append(row);
    }
    full.querySelectorAll("[data-confirm-order]").forEach((button) => {
        button.addEventListener("click", () => confirmOrder(Number(button.dataset.confirmOrder)));
    });
}

function renderLoadRanking() {
    const container = document.querySelector("#load-ranking");
    container.replaceChildren();
    const candidates = new Map(
        (state.bottleneckAnalysis?.candidates || [])
            .map((candidate) => [candidate.machineId, candidate])
    );
    const loads = [...state.capacity.entries()]
        .sort((left, right) => {
            const leftRank = candidates.get(left[0])?.rank
                ?? Number.MAX_SAFE_INTEGER;
            const rightRank = candidates.get(right[0])?.rank
                ?? Number.MAX_SAFE_INTEGER;
            return leftRank - rightRank
                || right[1].utilization - left[1].utilization;
        });
    if (loads.length === 0) {
        container.innerHTML = `<div class="load-empty">스케줄 실행 후 설비별<br>CAPA 사용률을 계산합니다.</div>`;
        return;
    }
    for (const [machineId, load] of loads) {
        const machine = state.machines.find((item) => item.id === machineId);
        const candidate = candidates.get(machineId);
        const utilization = candidate?.utilizationPercent
            ?? load.utilization;
        const utilizationLabel = candidate
            && candidate.utilizationPercent == null
            ? "CAPA 없음"
            : `${utilization}%`;
        const diagnosis = candidate
            ? `병목 #${candidate.rank} · ${bottleneckReason(
                candidate.reason
            )}`
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

function renderMasterData() {
    const facilities = document.querySelector("#facility-tree");
    const products = document.querySelector("#product-tree");
    facilities.replaceChildren();
    products.replaceChildren();

    if (state.factories.length === 0) {
        facilities.innerHTML = `<div class="table-empty">공장과 생산 자원을 등록해 주세요.</div>`;
    }
    for (const factory of state.factories) {
        const group = document.createElement("article");
        group.className = "tree-group";
        const lines = state.lines.filter((line) => line.factoryId === factory.id);
        group.innerHTML = `<div class="tree-group-header"><strong>${escapeHtml(factory.name)}</strong><span>${escapeHtml(factory.code)} · ${lines.length} LINES</span></div>`;
        for (const line of lines) {
            const machines = state.machines.filter((machine) => machine.productionLineId === line.id);
            const lineElement = document.createElement("div");
            lineElement.className = "tree-line";
            lineElement.innerHTML = `<strong>${escapeHtml(line.name)}</strong><div class="tree-children">${
                machines.map((machine) => `<span class="resource-chip ${machine.status.toLowerCase()}">${escapeHtml(machine.code)} · ${machine.status}</span>`).join("")
            }</div>`;
            group.append(lineElement);
        }
        facilities.append(group);
    }

    if (state.products.length === 0) {
        products.innerHTML = `<div class="table-empty">품목과 Routing을 등록해 주세요.</div>`;
    }
    for (const product of state.products) {
        const group = document.createElement("article");
        group.className = "tree-group";
        const routings = state.routings.filter((routing) => routing.productId === product.id);
        group.innerHTML = `<div class="tree-group-header"><strong>${escapeHtml(product.name)}</strong><span>${escapeHtml(product.code)} · ${product.unit}</span></div>`;
        const body = document.createElement("div");
        body.className = "tree-line";
        for (const routing of routings) {
            const card = document.createElement("div");
            card.className = "routing-card";
            card.innerHTML = `<strong>${escapeHtml(routing.code)} · ${escapeHtml(routing.name)}</strong>
                <div class="operation-chain">${routing.operations.map((operation, index) =>
                    `${index > 0 ? `<span class="operation-arrow">→</span>` : ""}<span class="operation-node">${escapeHtml(operation.code)} · ${operation.processingTimeMinutes}m</span>`
                ).join("")}</div>`;
            body.append(card);
        }
        if (routings.length === 0) {
            body.innerHTML = `<span class="load-empty">등록된 Routing이 없습니다.</span>`;
        }
        group.append(body);
        products.append(group);
    }
}

function populateSelects() {
    setOptions("#line-factory-select", state.factories, (item) => `${item.code} · ${item.name}`);
    setOptions("#machine-line-select", state.lines, (item) => `${item.code} · ${item.name}`);
    setOptions("#routing-product-select", state.products, (item) => `${item.code} · ${item.name}`);
    setOptions("#calendar-machine-select", state.machines.filter((item) => item.status !== "INACTIVE"), (item) => `${item.code} · ${item.name}`);
    const routingOptions = state.routings.map((routing) => {
        const product = state.products.find((item) => item.id === routing.productId);
        return {...routing, label: `${product?.code || "-"} / ${routing.code}`};
    });
    setOptions("#order-routing-select", routingOptions, (item) => item.label);
    document.querySelectorAll(".operation-row select[data-field='machineId']")
        .forEach((select) => fillSelect(select, state.machines.filter((item) => item.status === "AVAILABLE"), (item) => `${item.code} · ${item.name}`));
}

function bindNavigation() {
    document.querySelectorAll("[data-view]").forEach((button) => {
        button.addEventListener("click", () => showView(button.dataset.view));
    });
    document.querySelectorAll("[data-view-jump]").forEach((button) => {
        button.addEventListener("click", () => showView(button.dataset.viewJump));
    });
}

function showView(view) {
    const titles = {
        schedule: "생산 스케줄 보드",
        orders: "생산오더 관리",
        master: "마스터 데이터",
        guide: "APS 사용자 가이드"
    };
    document.querySelectorAll(".view").forEach((element) => element.classList.remove("is-active"));
    document.querySelector(`#${view}-view`)?.classList.add("is-active");
    document.querySelectorAll(".nav-item").forEach((item) =>
        item.classList.toggle("is-active", item.dataset.view === view));
    text("#view-title", titles[view]);
}

function bindDialogs() {
    document.querySelectorAll("[data-open-dialog]").forEach((button) => {
        button.addEventListener("click", () => {
            const dialog = document.querySelector(`#${button.dataset.openDialog}`);
            if (!dialog) return;
            if (dialog.id === "routing-dialog" && document.querySelectorAll(".operation-row").length === 0) {
                addOperationRow();
            }
            setDefaultDates();
            populateSelects();
            dialog.showModal();
            dialog.querySelector("input, select")?.focus();
        });
    });
    document.querySelectorAll(".modal").forEach((dialog) => {
        dialog.querySelectorAll(".modal-close, .modal-cancel").forEach((button) =>
            button.addEventListener("click", () => dialog.close()));
        dialog.addEventListener("click", (event) => {
            if (event.target === dialog) dialog.close();
        });
    });
}

function bindActions() {
    document.querySelector("#refresh-button").addEventListener("click", loadAll);
    document.querySelector("#add-operation-button").addEventListener("click", addOperationRow);
    document.querySelector("[data-guide-start]").addEventListener("click", () => {
        document.querySelector("#guide-onboarding").scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    });
    document.querySelectorAll("[data-sample-step]").forEach((button) => {
        button.addEventListener("click", () => runSampleStep(button.dataset.sampleStep));
    });
}

function bindForms() {
    bindForm("#schedule-form", async (form) => {
        const planningStart = new Date(new FormData(form).get("planningStart")).toISOString();
        await request(API.schedules, {
            method: "POST",
            body: JSON.stringify({executionKey: crypto.randomUUID(), planningStart})
        });
        await loadAll();
        return "스케줄 계산과 결과 저장을 완료했습니다.";
    });
    bindForm("#factory-form", async (form) => {
        await request(API.factories, {method: "POST", body: JSON.stringify(values(form, ["code", "name"]))});
        await loadAll();
        return "공장을 등록했습니다.";
    });
    bindForm("#line-form", async (form) => {
        const data = values(form, ["factoryId", "code", "name"]);
        await request(API.lines(Number(data.factoryId)), {method: "POST", body: JSON.stringify({code: data.code, name: data.name})});
        await loadAll();
        return "생산라인을 등록했습니다.";
    });
    bindForm("#machine-form", async (form) => {
        const data = values(form, ["productionLineId", "code", "name", "status"]);
        await request(API.machines(Number(data.productionLineId)), {method: "POST", body: JSON.stringify({code: data.code, name: data.name, status: data.status})});
        await loadAll();
        return "설비를 등록했습니다.";
    });
    bindForm("#product-form", async (form) => {
        await request(API.products, {method: "POST", body: JSON.stringify(values(form, ["code", "name", "unit"]))});
        await loadAll();
        return "품목을 등록했습니다.";
    });
    bindForm("#routing-form", async (form) => {
        const data = values(form, ["productId", "code", "name"]);
        const operations = [...form.querySelectorAll(".operation-row")].map((row) => ({
            sequence: Number(row.querySelector("[data-field='sequence']").value),
            code: row.querySelector("[data-field='code']").value,
            name: row.querySelector("[data-field='name']").value,
            processingTimeMinutes: Number(row.querySelector("[data-field='processingTimeMinutes']").value),
            machineId: Number(row.querySelector("[data-field='machineId']").value)
        }));
        await request(API.routings(Number(data.productId)), {method: "POST", body: JSON.stringify({code: data.code, name: data.name, operations})});
        document.querySelector("#operation-rows").replaceChildren();
        await loadAll();
        return "Routing과 공정을 등록했습니다.";
    });
    bindForm("#order-form", async (form) => {
        const formData = new FormData(form);
        const created = await request(API.orders, {
            method: "POST",
            body: JSON.stringify({
                orderNumber: formData.get("orderNumber"),
                routingId: Number(formData.get("routingId")),
                quantity: Number(formData.get("quantity")),
                releaseAt: new Date(formData.get("releaseAt")).toISOString(),
                dueAt: new Date(formData.get("dueAt")).toISOString(),
                priority: Number(formData.get("priority"))
            })
        });
        if (formData.get("confirm")) {
            await request(API.confirmOrder(created.id), {method: "POST"});
        }
        await loadAll();
        return "생산오더를 등록했습니다.";
    });
    bindForm("#calendar-form", async (form) => {
        const data = values(form, ["machineId", "startTime", "endTime"]);
        const days = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"];
        await request(API.calendars(Number(data.machineId)), {
            method: "POST",
            body: JSON.stringify({entries: days.map((dayOfWeek) => ({
                dayOfWeek,
                startTime: `${data.startTime}:00`,
                endTime: `${data.endTime}:00`
            }))})
        });
        await loadAll();
        return "월~금 근무시간을 등록했습니다.";
    });
}

function bindForm(selector, action) {
    const form = document.querySelector(selector);
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const submit = form.querySelector("[type='submit']");
        submit.disabled = true;
        try {
            const message = await action(form);
            form.reset();
            form.closest("dialog").close();
            setDefaultDates();
            showToast(message);
        } catch (error) {
            showToast(error.message, true);
        } finally {
            submit.disabled = false;
        }
    });
}

async function confirmOrder(orderId) {
    try {
        await request(API.confirmOrder(orderId), {method: "POST"});
        await loadAll();
        showToast("생산오더를 확정했습니다.");
    } catch (error) {
        showToast(error.message, true);
    }
}

async function runSampleStep(stepKey) {
    if (runningSampleStep !== null) return;

    const completion = sampleCompletion();
    if (stepKey === "schedule" && completion.schedule) {
        showView("schedule");
        return;
    }
    const stepIndex = SAMPLE_STEP_KEYS.indexOf(stepKey);
    const missingPreviousStep = SAMPLE_STEP_KEYS
        .slice(0, stepIndex)
        .some((key) => !completion[key]);
    if (stepIndex < 0 || missingPreviousStep || completion[stepKey]) {
        return;
    }

    runningSampleStep = stepKey;
    renderSampleOnboarding();
    setGuideLog({
        resources: "공장부터 근무시간까지 순서대로 확인하고 있습니다.",
        process: "품목과 Routing, 공정별 설비 연결을 등록하고 있습니다.",
        orders: "샘플 오더 2건을 등록하고 CONFIRMED 상태로 확정하고 있습니다.",
        schedule: "확정 오더를 읽어 설비별 작업 시간을 계산하고 있습니다."
    }[stepKey]);

    try {
        await {
            resources: createSampleResources,
            process: createSampleProcess,
            orders: createSampleOrders,
            schedule: executeSampleSchedule
        }[stepKey]();
        await loadAll();
        const message = {
            resources: "1단계 완료: 생산 자원과 근무시간을 준비했습니다.",
            process: "2단계 완료: 품목과 Routing을 준비했습니다.",
            orders: "3단계 완료: 샘플 오더 2건을 확정했습니다.",
            schedule: "4단계 완료: 샘플 스케줄을 생성했습니다."
        }[stepKey];
        setGuideLog(message);
        showToast(message);
        if (stepKey === "schedule") {
            showView("schedule");
        }
    } catch (error) {
        try {
            await loadAll();
        } catch {
            // loadAll이 연결 상태와 오류 메시지를 화면에 반영합니다.
        }
        setGuideLog(`진행을 멈췄습니다: ${error.message}`);
        showToast(error.message, true);
    } finally {
        runningSampleStep = null;
        renderSampleOnboarding();
    }
}

async function createSampleResources() {
    let factory = state.factories.find(
        (item) => item.code === SAMPLE_DATA.factory.code
    );
    if (!factory) {
        setGuideLog("1/6 데모 공장을 등록하고 있습니다.");
        factory = await request(API.factories, {
            method: "POST",
            body: JSON.stringify(SAMPLE_DATA.factory)
        });
    }
    if (!factory.active) {
        throw new Error("기존 데모 공장이 비활성 상태라 샘플을 이어갈 수 없습니다.");
    }

    let line = state.lines.find(
        (item) => item.factoryId === factory.id && item.code === SAMPLE_DATA.line.code
    );
    if (!line) {
        setGuideLog("2/6 데모 공장 아래에 기본 생산라인을 등록하고 있습니다.");
        line = await request(API.lines(factory.id), {
            method: "POST",
            body: JSON.stringify(SAMPLE_DATA.line)
        });
    }
    if (!line.active) {
        throw new Error("기존 데모 생산라인이 비활성 상태라 샘플을 이어갈 수 없습니다.");
    }

    const machines = [];
    for (let index = 0; index < SAMPLE_DATA.machines.length; index += 1) {
        const sampleMachine = SAMPLE_DATA.machines[index];
        let machine = state.machines.find(
            (item) => item.productionLineId === line.id && item.code === sampleMachine.code
        );
        if (!machine) {
            setGuideLog(`${index + 3}/6 ${sampleMachine.name}를 등록하고 있습니다.`);
            machine = await request(API.machines(line.id), {
                method: "POST",
                body: JSON.stringify(sampleMachine)
            });
        }
        if (machine.status !== "AVAILABLE") {
            throw new Error(`${machine.name}가 AVAILABLE 상태가 아닙니다.`);
        }
        machines.push(machine);
    }

    for (let index = 0; index < machines.length; index += 1) {
        const machine = machines[index];
        setGuideLog(`${index + 5}/6 ${machine.name}의 월~금 근무시간을 확인하고 있습니다.`);
        const existing = await request(API.calendars(machine.id));
        const registeredDays = new Set(
            existing.filter((calendar) => calendar.active)
                .map((calendar) => calendar.dayOfWeek)
        );
        const missingDays = SAMPLE_DATA.weekdays.filter(
            (dayOfWeek) => !registeredDays.has(dayOfWeek)
        );
        if (missingDays.length > 0) {
            await request(API.calendars(machine.id), {
                method: "POST",
                body: JSON.stringify({
                    entries: missingDays.map((dayOfWeek) => ({
                        dayOfWeek,
                        startTime: "08:00:00",
                        endTime: "17:00:00"
                    }))
                })
            });
        }
    }
}

async function createSampleProcess() {
    const before = sampleEntities();
    if (!sampleCompletion().resources) {
        throw new Error("먼저 샘플 생산 자원을 준비해 주세요.");
    }

    let product = before.product;
    if (!product) {
        setGuideLog("1/2 완제품 A를 등록하고 있습니다.");
        product = await request(API.products, {
            method: "POST",
            body: JSON.stringify(SAMPLE_DATA.product)
        });
    }
    if (!product.active) {
        throw new Error("기존 데모 품목이 비활성 상태라 샘플을 이어갈 수 없습니다.");
    }

    const existingRouting = state.routings.find(
        (item) => item.productId === product.id && item.code === SAMPLE_DATA.routing.code
    );
    if (existingRouting) {
        if (!isExpectedSampleRouting(existingRouting, before.machines)) {
            throw new Error("기존 데모 Routing의 공정 구성이 샘플과 다릅니다.");
        }
        return;
    }

    setGuideLog("2/2 절단 → 조립 Routing을 등록하고 있습니다.");
    const machinesByCode = new Map(
        before.machines.map((machine) => [machine.code, machine])
    );
    const operations = SAMPLE_DATA.routing.operations.map((operation) => ({
        sequence: operation.sequence,
        code: operation.code,
        name: operation.name,
        processingTimeMinutes: operation.processingTimeMinutes,
        machineId: machinesByCode.get(operation.machineCode).id
    }));
    await request(API.routings(product.id), {
        method: "POST",
        body: JSON.stringify({
            code: SAMPLE_DATA.routing.code,
            name: SAMPLE_DATA.routing.name,
            operations
        })
    });
}

async function createSampleOrders() {
    const context = sampleEntities();
    if (!sampleCompletion().process) {
        throw new Error("먼저 샘플 품목과 공정을 준비해 주세요.");
    }

    const {releaseAt} = samplePlanningDates();
    for (let index = 0; index < SAMPLE_DATA.orders.length; index += 1) {
        const sampleOrder = SAMPLE_DATA.orders[index];
        let order = state.orders.find(
            (item) => item.orderNumber === sampleOrder.orderNumber
        );
        if (!order) {
            setGuideLog(`${index + 1}/2 ${sampleOrder.orderNumber} 오더를 등록하고 있습니다.`);
            const dueAt = addWorkingDays(releaseAt, sampleOrder.dueWorkingDays);
            dueAt.setHours(17, 0, 0, 0);
            order = await request(API.orders, {
                method: "POST",
                body: JSON.stringify({
                    orderNumber: sampleOrder.orderNumber,
                    routingId: context.routing.id,
                    quantity: sampleOrder.quantity,
                    releaseAt: releaseAt.toISOString(),
                    dueAt: dueAt.toISOString(),
                    priority: sampleOrder.priority
                })
            });
        }
        if (order.routingId !== context.routing.id) {
            throw new Error(`${order.orderNumber} 오더가 다른 Routing을 사용하고 있습니다.`);
        }
        if (order.status === "DRAFT") {
            await request(API.confirmOrder(order.id), {method: "POST"});
        } else if (!["CONFIRMED", "SCHEDULED"].includes(order.status)) {
            throw new Error(`${order.orderNumber} 오더가 ${order.status} 상태라 사용할 수 없습니다.`);
        }
    }
}

async function executeSampleSchedule() {
    const context = sampleEntities();
    if (!sampleCompletion().orders) {
        throw new Error("먼저 샘플 생산오더를 준비해 주세요.");
    }
    const confirmedOrders = context.orders.filter(
        (order) => order?.status === "CONFIRMED"
    );
    if (confirmedOrders.length === 0) {
        throw new Error("이미 샘플 오더의 스케줄 실행이 완료되었습니다.");
    }
    const planningStart = confirmedOrders
        .map((order) => new Date(order.releaseAt))
        .sort((left, right) => left - right)[0];
    await request(API.schedules, {
        method: "POST",
        body: JSON.stringify({
            executionKey: crypto.randomUUID(),
            planningStart: planningStart.toISOString()
        })
    });
}

function sampleEntities() {
    const factory = state.factories.find(
        (item) => item.code === SAMPLE_DATA.factory.code
    );
    const line = factory && state.lines.find(
        (item) => item.factoryId === factory.id && item.code === SAMPLE_DATA.line.code
    );
    const machines = SAMPLE_DATA.machines.map((sampleMachine) =>
        line && state.machines.find(
            (item) =>
                item.productionLineId === line.id
                && item.code === sampleMachine.code
        )
    );
    const product = state.products.find(
        (item) => item.code === SAMPLE_DATA.product.code
    );
    const routing = product && state.routings.find(
        (item) =>
            item.productId === product.id
            && item.code === SAMPLE_DATA.routing.code
    );
    const orders = SAMPLE_DATA.orders.map((sampleOrder) =>
        state.orders.find(
            (item) => item.orderNumber === sampleOrder.orderNumber
        )
    );
    return {factory, line, machines, product, routing, orders};
}

function sampleCompletion() {
    const context = sampleEntities();
    const resources = Boolean(
        context.factory?.active
        && context.line?.active
        && context.machines.every((machine) =>
            machine?.status === "AVAILABLE"
            && hasWeekdayCalendar(machine.id)
        )
    );
    const process = Boolean(
        resources
        && context.product?.active
        && context.routing?.active
        && isExpectedSampleRouting(context.routing, context.machines)
    );
    const orders = Boolean(
        process
        && context.orders.every((order) =>
            order
            && order.routingId === context.routing.id
            && ["CONFIRMED", "SCHEDULED"].includes(order.status)
        )
    );
    const schedule = Boolean(
        orders
        && context.orders.every((order) => order.status === "SCHEDULED")
    );
    return {resources, process, orders, schedule};
}

function hasWeekdayCalendar(machineId) {
    const registeredDays = new Set(
        (state.sampleCalendars.get(machineId) || [])
            .filter((calendar) => calendar.active)
            .map((calendar) => calendar.dayOfWeek)
    );
    return SAMPLE_DATA.weekdays.every((day) => registeredDays.has(day));
}

function isExpectedSampleRouting(routing, machines) {
    if (!routing || machines.some((machine) => !machine)) return false;
    if (routing.operations.length !== SAMPLE_DATA.routing.operations.length) {
        return false;
    }
    const machineIdsByCode = new Map(
        machines.map((machine) => [machine.code, machine.id])
    );
    return SAMPLE_DATA.routing.operations.every((expected) =>
        routing.operations.some((operation) =>
            operation.sequence === expected.sequence
            && operation.code === expected.code
            && operation.processingTimeMinutes === expected.processingTimeMinutes
            && operation.machineId === machineIdsByCode.get(expected.machineCode)
        )
    );
}

function samplePlanningDates() {
    const releaseAt = new Date();
    releaseAt.setDate(releaseAt.getDate() + 1);
    releaseAt.setHours(8, 0, 0, 0);
    while ([0, 6].includes(releaseAt.getDay())) {
        releaseAt.setDate(releaseAt.getDate() + 1);
    }
    return {releaseAt};
}

function addWorkingDays(value, days) {
    const result = new Date(value);
    let remaining = days;
    while (remaining > 0) {
        result.setDate(result.getDate() + 1);
        if (![0, 6].includes(result.getDay())) {
            remaining -= 1;
        }
    }
    return result;
}

function setGuideLog(message) {
    guideLogMessage = message;
    text("#guide-sample-log", message);
}

function addOperationRow() {
    const template = document.querySelector("#operation-row-template");
    const row = template.content.firstElementChild.cloneNode(true);
    const count = document.querySelectorAll(".operation-row").length;
    row.querySelector("[data-field='sequence']").value = (count + 1) * 10;
    fillSelect(row.querySelector("select"), state.machines.filter((item) => item.status === "AVAILABLE"), (item) => `${item.code} · ${item.name}`);
    row.querySelector(".remove-operation").addEventListener("click", () => row.remove());
    document.querySelector("#operation-rows").append(row);
}

function setDefaultDates() {
    const start = new Date();
    start.setMinutes(0, 0, 0);
    start.setHours(start.getHours() + 1);
    const due = new Date(start);
    due.setDate(due.getDate() + 3);
    setInputIfEmpty("#schedule-start-input", toLocalInput(start));
    setInputIfEmpty("#order-release-input", toLocalInput(start));
    setInputIfEmpty("#order-due-input", toLocalInput(due));
}

function setOptions(selector, items, label) {
    const select = document.querySelector(selector);
    if (select) fillSelect(select, items, label);
}

function fillSelect(select, items, label) {
    const current = select.value;
    select.replaceChildren();
    if (items.length === 0) {
        const option = new Option("선택 가능한 항목 없음", "");
        option.disabled = true;
        option.selected = true;
        select.append(option);
        return;
    }
    for (const item of items) {
        select.append(new Option(label(item), item.id));
    }
    if (items.some((item) => String(item.id) === current)) {
        select.value = current;
    }
}

function setConnection(status) {
    const element = document.querySelector("#connection-status");
    element.className = `connection-status ${status === "online" ? "" : `is-${status}`}`.trim();
    element.querySelector("strong").textContent = {
        online: "API ONLINE",
        offline: "API OFFLINE",
        checking: "서버 확인 중"
    }[status];
}

function compareOrders(left, right) {
    const statusRank = {CONFIRMED: 0, DRAFT: 1, SCHEDULED: 2, CANCELLED: 3};
    return (statusRank[left.status] ?? 9) - (statusRank[right.status] ?? 9)
        || right.priority - left.priority
        || new Date(left.dueAt) - new Date(right.dueAt);
}

function statusBadge(status) {
    return `<span class="status-pill ${status.toLowerCase()}">${escapeHtml(status)}</span>`;
}

function groupBy(items, keyFunction) {
    const groups = new Map();
    for (const item of items) {
        const key = keyFunction(item);
        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(item);
    }
    return groups;
}

function colorFor(id) {
    return colors[Math.abs(Number(id)) % colors.length];
}

function values(form, fields) {
    const data = new FormData(form);
    return Object.fromEntries(fields.map((field) => [field, data.get(field)]));
}

function text(selector, value) {
    document.querySelector(selector).textContent = value;
}

function formatDateTime(value) {
    if (!value) return "-";
    const parts = offsetDateParts(value, displayOffsetSeconds());
    return `${pad(parts.month)}. ${pad(parts.day)}. ${pad(parts.hour)}:${pad(parts.minute)}`;
}

function formatTime(value) {
    const parts = offsetDateParts(value, displayOffsetSeconds());
    return `${pad(parts.hour)}:${pad(parts.minute)}`;
}

function formatAxisTime(value) {
    const parts = offsetDateParts(value, displayOffsetSeconds());
    return `${pad(parts.month)}. ${pad(parts.day)}. ${pad(parts.hour)}:${pad(parts.minute)}`;
}

function displayOffsetSeconds() {
    return state.latestSchedule?.planningOffsetSeconds
        ?? -new Date().getTimezoneOffset() * 60;
}

function withOffset(value, offsetSeconds) {
    const parts = offsetDateParts(value, offsetSeconds);
    const sign = offsetSeconds >= 0 ? "+" : "-";
    const absoluteMinutes = Math.abs(offsetSeconds) / 60;
    const offsetHour = Math.floor(absoluteMinutes / 60);
    const offsetMinute = absoluteMinutes % 60;
    return `${parts.year}-${pad(parts.month)}-${pad(parts.day)}T${pad(parts.hour)}:${pad(parts.minute)}:${pad(parts.second)}${sign}${pad(offsetHour)}:${pad(offsetMinute)}`;
}

function offsetDateParts(value, offsetSeconds) {
    const date = value instanceof Date ? value : new Date(value);
    const shifted = new Date(date.getTime() + offsetSeconds * 1000);
    return {
        year: shifted.getUTCFullYear(),
        month: shifted.getUTCMonth() + 1,
        day: shifted.getUTCDate(),
        hour: shifted.getUTCHours(),
        minute: shifted.getUTCMinutes(),
        second: shifted.getUTCSeconds()
    };
}

function pad(value) {
    return String(value).padStart(2, "0");
}

function toLocalInput(date) {
    const offset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

function setInputIfEmpty(selector, value) {
    const input = document.querySelector(selector);
    if (input && !input.value) input.value = value;
}

function number(value) {
    return new Intl.NumberFormat("ko-KR").format(value);
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function showToast(message, isError = false) {
    window.clearTimeout(toastTimer);
    const toast = document.querySelector("#toast");
    toast.textContent = message;
    toast.classList.toggle("is-error", isError);
    toast.classList.add("is-visible");
    toastTimer = window.setTimeout(() => toast.classList.remove("is-visible"), 3600);
}
