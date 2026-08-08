import {API, request} from "./js/api.js";
import {SAMPLE_DATA, SAMPLE_STEP_KEYS} from "./js/guide-data.js";
import {
    renderGuideStatus,
    renderLearningScenarios,
    renderConstraintImpact,
    renderFrozenHorizonLab,
    renderRuleComparison,
    renderSampleOnboarding as renderGuideOnboarding
} from "./js/guide.js";
import {renderMasterData, populateSelects} from "./js/master-data.js";
import {renderOrders} from "./js/orders.js";
import {renderScheduleBoard} from "./js/schedule-board.js";
import {state} from "./js/state.js";
import {
    escapeHtml,
    fillSelect,
    formatDateTime,
    number,
    setConnection,
    setInputIfEmpty,
    showToast,
    text,
    toLocalInput,
    values,
    withOffset
} from "./js/ui.js";

let runningSampleStep = null;
let guideLogMessage = null;
let csvImportRequestKey = crypto.randomUUID();
let csvPreviewReady = false;

document.addEventListener("DOMContentLoaded", async () => {
    bindNavigation();
    bindDialogs();
    bindForms();
    bindActions();
    setDefaultDates();
    await loadAll();
});

async function loadAll() {
    setConnection("checking");
    try {
        const [factoryPage, productPage, orderPage, latestSchedule, learningScenarios] =
            await Promise.all([
                request(`${API.factories}?page=0&size=100`),
                request(`${API.products}?page=0&size=100`),
                request(`${API.orders}?page=0&size=100`),
                request(API.latestSchedule, {allowNotFound: true}),
                request(API.learningScenarios)
            ]);
        state.factories = factoryPage.content;
        state.products = productPage.content;
        state.orders = orderPage.content;
        state.latestSchedule = latestSchedule;
        state.learningScenarios = learningScenarios;

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
    renderScheduleBoard();
    renderOrders(confirmOrder);
    renderMasterData();
    renderGuideStatus();
    renderLearningScenarios();
    renderRuleComparison();
    renderConstraintImpact();
    renderFrozenHorizonLab();
    renderSampleOnboarding();
    populateSelects();
}
function renderSampleOnboarding() {
    const completion = sampleCompletion();
    renderGuideOnboarding({
        completion,
        runningSampleStep,
        guideLogMessage,
        setGuideLog
    });
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
            if (dialog.id === "reschedule-dialog") {
                text(
                    "#reschedule-source-run",
                    state.latestSchedule
                        ? `RUN #${state.latestSchedule.id}`
                        : "선택된 실행 없음"
                );
                dialog.querySelector("[name='dispatchingRule']").value =
                    state.latestSchedule?.dispatchingRule
                    || "EXPLICIT_PRIORITY";
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
        document.querySelector("#guide-scenario-labs").scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
    });
    document.querySelectorAll("[data-guide-target]").forEach((button) => {
        button.addEventListener("click", () => {
            document.getElementById(button.dataset.guideTarget)?.scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
        });
    });
    document.querySelectorAll("[data-sample-step]").forEach((button) => {
        button.addEventListener("click", () => runSampleStep(button.dataset.sampleStep));
    });
    document.querySelector("#guide-scenario-grid").addEventListener(
        "click",
        (event) => {
            const button = event.target.closest("[data-learning-scenario]");
            if (button) runLearningScenario(button.dataset.learningScenario);
        }
    );
    document.querySelector("#guide-rule-comparison").addEventListener(
        "click",
        (event) => {
            const button = event.target.closest("[data-confirm-learning-rule]");
            if (button) confirmLearningRule(
                button.dataset.confirmLearningRule
            );
        }
    );
    bindCsvPreview();
}

async function runLearningScenario(scenarioKey) {
    if (state.runningLearningScenario) return;
    state.runningLearningScenario = scenarioKey;
    renderLearningScenarios();
    try {
        const instance = await request(
            API.learningScenarioInstances(scenarioKey),
            {
                method: "POST",
                body: JSON.stringify({requestKey: crypto.randomUUID()})
            }
        );
        if (scenarioKey === "FROZEN_HORIZON") {
            const lab = await request(API.learningFrozenHorizon(instance.id), {
                method: "POST",
                body: JSON.stringify({
                    baselineExecutionKey: crypto.randomUUID(),
                    rescheduleExecutionKey: crypto.randomUUID(),
                    dispatchingRule: "EXPLICIT_PRIORITY"
                })
            });
            state.learningInstance = instance;
            state.learningComparison = null;
            state.constraintImpact = null;
            state.frozenHorizonLab = lab;
            await loadAll();
            document.querySelector("#guide-frozen-horizon").scrollIntoView({
                behavior: "smooth",
                block: "start"
            });
            showToast("Frozen Horizon 재계획 전후를 만들었습니다.");
            return;
        }
        const [comparison, constraintImpact] = await Promise.all([
            request(API.learningRuleComparison(instance.id)),
            request(API.learningConstraintImpact(instance.id))
        ]);
        state.learningInstance = instance;
        state.learningComparison = comparison;
        state.constraintImpact = constraintImpact;
        state.frozenHorizonLab = null;
        renderRuleComparison();
        renderConstraintImpact();
        document.querySelector("#guide-rule-comparison").scrollIntoView({
            behavior: "smooth",
            block: "start"
        });
        showToast(`${scenarioKey}의 세 규칙 비교를 완료했습니다.`);
    } catch (error) {
        showToast(error.message, true);
    } finally {
        state.runningLearningScenario = null;
        renderLearningScenarios();
    }
}

async function confirmLearningRule(dispatchingRule) {
    if (!state.learningInstance || state.runningLearningScenario) return;
    state.runningLearningScenario = "CONFIRM";
    renderLearningScenarios();
    try {
        await submitScheduleExecution(
            API.learningInstanceSchedules(state.learningInstance.id),
            {
                executionKey: crypto.randomUUID(),
                dispatchingRule
            }
        );
        showToast(`${dispatchingRule} 규칙으로 실습 계획을 확정했습니다.`);
        await loadAll();
        showView("schedule");
    } catch (error) {
        showToast(error.message, true);
    } finally {
        state.runningLearningScenario = null;
        renderLearningScenarios();
    }
}

function bindCsvPreview() {
    const form = document.querySelector("#csv-preview-form");
    const fileInput = form.querySelector("[name='file']");
    const applyButton = document.querySelector("#csv-apply-button");
    fileInput.addEventListener("change", () => {
        csvImportRequestKey = crypto.randomUUID();
        csvPreviewReady = false;
        applyButton.disabled = true;
    });
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const submit = form.querySelector("[type='submit']");
        submit.disabled = true;
        try {
            const response = await request(API.planningDataPreview, {
                method: "POST",
                body: new FormData(form)
            });
            csvPreviewReady = response.readyToApply;
            applyButton.disabled = !csvPreviewReady;
            renderCsvPreview(response);
            showToast(response.readyToApply
                ? "모든 CSV 행이 검증을 통과했습니다."
                : "수정이 필요한 CSV 행이 있습니다.",
            !response.readyToApply);
        } catch (error) {
            showToast(error.message, true);
        } finally {
            submit.disabled = false;
        }
    });
    applyButton.addEventListener("click", async () => {
        if (!csvPreviewReady || fileInput.files.length === 0) {
            showToast("먼저 CSV 검증을 완료해 주세요.", true);
            return;
        }
        applyButton.disabled = true;
        try {
            const query = new URLSearchParams({
                requestKey: csvImportRequestKey
            });
            const response = await request(
                `${API.planningDataImports}?${query}`,
                {
                    method: "POST",
                    body: new FormData(form)
                }
            );
            renderCsvImportRun(response);
            const completed = response.status === "COMPLETED";
            const interrupted = response.status === "INTERRUPTED";
            csvPreviewReady = interrupted;
            applyButton.disabled = !interrupted;
            showToast(
                completed
                    ? `${number(response.successRows)}개 행을 모두 반영했습니다.`
                    : response.failureReason
                        || "CSV 반영 결과를 확인해 주세요.",
                !completed
            );
            if (completed) await loadAll();
        } catch (error) {
            applyButton.disabled = false;
            showToast(error.message, true);
        }
    });
}

function renderCsvPreview(preview) {
    text("#csv-total-rows", number(preview.totalRows));
    text("#csv-valid-rows", number(preview.validRows));
    text("#csv-invalid-rows", number(preview.invalidRows));
    const status = document.querySelector("#csv-preview-status");
    status.textContent = preview.readyToApply ? "반영 준비 완료" : "수정 필요";
    status.className = `status-pill ${preview.readyToApply ? "completed" : "delayed"}`;
    const body = document.querySelector("#csv-preview-body");
    body.innerHTML = preview.rows.map((row) => {
        const normalized = Object.entries(row.normalizedValues)
            .map(([key, value]) => `${escapeHtml(key)}=${escapeHtml(value)}`)
            .join("<br>") || "-";
        const errors = row.errors.length === 0
            ? "정상"
            : row.errors
                .map((error) =>
                    `[${escapeHtml(error.field)}] ${escapeHtml(error.message)}`)
                .join("<br>");
        return `<tr>
            <td>${row.rowNumber}</td>
            <td>${escapeHtml(row.type || "-")}</td>
            <td>${normalized}</td>
            <td class="${row.valid ? "" : "text-danger"}">${errors}</td>
        </tr>`;
    }).join("");
}

function renderCsvImportRun(run) {
    text("#csv-total-rows", number(run.totalRows));
    text("#csv-valid-rows", number(run.successRows));
    text(
        "#csv-invalid-rows",
        number(run.failedRows + run.skippedRows)
    );
    const status = document.querySelector("#csv-preview-status");
    const labels = {
        RUNNING: "반영 중",
        COMPLETED: "반영 완료",
        FAILED: "반영 실패",
        INTERRUPTED: "재시도 가능"
    };
    status.textContent = labels[run.status] || run.status;
    status.className = `status-pill ${
        run.status === "COMPLETED"
            ? "completed"
            : run.status === "RUNNING"
                ? "running"
                : "delayed"
    }`;
    const body = document.querySelector("#csv-preview-body");
    body.innerHTML = run.rows.length === 0
        ? `<tr><td colspan="4" class="empty-cell">${
            escapeHtml(run.failureReason || "반영 결과를 준비 중입니다.")
        }</td></tr>`
        : run.rows.map((row) => {
            const errors = row.errors.length === 0
                ? "반영 완료"
                : row.errors
                    .map((error) =>
                        `[${escapeHtml(error.code)}] ${
                            escapeHtml(error.message)
                        }`)
                    .join("<br>");
            return `<tr>
                <td>${row.rowNumber}</td>
                <td>${escapeHtml(row.type || "-")}</td>
                <td>-</td>
                <td class="${
                    row.status === "SUCCEEDED" ? "" : "text-danger"
                }">${errors}</td>
            </tr>`;
        }).join("");
}

function bindForms() {
    bindForm("#schedule-form", async (form) => {
        const formData = new FormData(form);
        const planningStart =
            new Date(formData.get("planningStart")).toISOString();
        await submitScheduleExecution(
            API.schedules,
            {
                executionKey: crypto.randomUUID(),
                planningStart,
                dispatchingRule: formData.get("dispatchingRule")
            }
        );
        await loadAll();
        return "스케줄 계산과 결과 저장을 완료했습니다.";
    });
    bindForm("#reschedule-form", async (form) => {
        const source = state.latestSchedule;
        if (!source) {
            throw new Error("재스케줄링할 원본 실행이 없습니다.");
        }
        const formData = new FormData(form);
        await submitScheduleExecution(
            API.reschedule(source.id),
            {
                executionKey: crypto.randomUUID(),
                frozenAt: new Date(
                    formData.get("frozenAt")
                ).toISOString(),
                dispatchingRule: formData.get("dispatchingRule")
            }
        );
        await loadAll();
        return "동결 작업을 유지한 새 스케줄을 저장했습니다.";
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
    await submitScheduleExecution(
        API.schedules,
        {
            executionKey: crypto.randomUUID(),
            planningStart: planningStart.toISOString(),
            dispatchingRule: "EXPLICIT_PRIORITY"
        }
    );
}

async function submitScheduleExecution(url, payload) {
    const submitted = await request(url, {
        method: "POST",
        body: JSON.stringify(payload)
    });
    return waitForScheduleExecution(submitted);
}

async function waitForScheduleExecution(initialExecution) {
    let execution = initialExecution;
    for (let attempt = 0; attempt < 240; attempt += 1) {
        if (execution.status === "COMPLETED") {
            if (!execution.resultScheduleRunId) {
                throw new Error("완료된 실행의 스케줄 결과를 찾을 수 없습니다.");
            }
            return execution;
        }
        if (execution.status === "FAILED") {
            throw new Error(
                execution.failureReason || "스케줄 계산에 실패했습니다."
            );
        }
        await new Promise((resolve) => setTimeout(resolve, 500));
        execution = await request(
            API.scheduleExecution(execution.id)
        );
    }
    throw new Error(
        "스케줄 계산이 2분 안에 끝나지 않았습니다. 실행 이력을 확인해 주세요."
    );
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
    setInputIfEmpty("#frozen-at-input", toLocalInput(start));
    setInputIfEmpty("#order-release-input", toLocalInput(start));
    setInputIfEmpty("#order-due-input", toLocalInput(due));
}
