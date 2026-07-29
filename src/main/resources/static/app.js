const API = {
    factories: "/api/v1/factories",
    lines: (factoryId) =>
        `/api/v1/factories/${factoryId}/production-lines`,
    machines: (productionLineId) =>
        `/api/v1/production-lines/${productionLineId}/machines`
};

const state = {
    factories: [],
    lines: [],
    machines: [],
    selectedFactoryId: null,
    selectedLineId: null
};

const elements = {
    connectionStatus: document.querySelector("#connection-status"),
    factoryCount: document.querySelector("#factory-count"),
    lineCount: document.querySelector("#line-count"),
    machineCount: document.querySelector("#machine-count"),
    availableCount: document.querySelector("#available-count"),
    factoryList: document.querySelector("#factory-list"),
    lineList: document.querySelector("#line-list"),
    machineList: document.querySelector("#machine-list"),
    openLineDialog: document.querySelector("#open-line-dialog"),
    openMachineDialog: document.querySelector("#open-machine-dialog"),
    lineFactoryName: document.querySelector("#line-factory-name"),
    machineLineName: document.querySelector("#machine-line-name"),
    toast: document.querySelector("#toast")
};

let toastTimer;

document.addEventListener("DOMContentLoaded", () => {
    bindDialogs();
    bindForms();
    loadFactories();
});

async function request(url, options = {}) {
    const response = await fetch(url, {
        headers: {
            "Content-Type": "application/json",
            ...options.headers
        },
        ...options
    });

    if (!response.ok) {
        const error = await response.json().catch(() => null);
        throw new Error(error?.message || `요청에 실패했습니다. (${response.status})`);
    }

    if (response.status === 204) {
        return null;
    }
    return response.json();
}

async function loadFactories(preferredFactoryId = null) {
    try {
        const page = await request(`${API.factories}?page=0&size=100`);
        state.factories = page.content;
        setConnection(true);

        const nextFactoryId = resolveSelectedId(
            state.factories,
            preferredFactoryId ?? state.selectedFactoryId
        );
        await selectFactory(nextFactoryId);
    } catch (error) {
        setConnection(false);
        state.factories = [];
        state.lines = [];
        state.machines = [];
        state.selectedFactoryId = null;
        state.selectedLineId = null;
        renderAll();
        showToast(error.message, true);
    }
}

async function selectFactory(factoryId) {
    state.selectedFactoryId = factoryId;
    state.selectedLineId = null;
    state.lines = [];
    state.machines = [];
    renderAll();

    if (factoryId === null) {
        return;
    }

    try {
        const page = await request(`${API.lines(factoryId)}?page=0&size=100`);
        state.lines = page.content;
        const nextLineId = resolveSelectedId(state.lines, null);
        await selectLine(nextLineId);
    } catch (error) {
        renderAll();
        showToast(error.message, true);
    }
}

async function selectLine(productionLineId) {
    state.selectedLineId = productionLineId;
    state.machines = [];
    renderAll();

    if (productionLineId === null) {
        return;
    }

    try {
        const page = await request(
            `${API.machines(productionLineId)}?page=0&size=100`
        );
        state.machines = page.content;
        renderAll();
    } catch (error) {
        renderAll();
        showToast(error.message, true);
    }
}

function resolveSelectedId(resources, preferredId) {
    if (resources.length === 0) {
        return null;
    }
    if (resources.some((resource) => resource.id === preferredId)) {
        return preferredId;
    }
    return resources[0].id;
}

function renderAll() {
    renderFactories();
    renderLines();
    renderMachines();
    renderMetrics();
    renderDialogContext();
}

function renderFactories() {
    renderResourceList({
        container: elements.factoryList,
        resources: state.factories,
        selectedId: state.selectedFactoryId,
        emptyTitle: "등록된 공장이 없습니다",
        emptyDescription: "오른쪽 위 + 버튼으로 첫 공장을 등록해 주세요.",
        onSelect: selectFactory,
        itemMeta: (factory) => factory.active ? "운영 중" : "비활성"
    });
}

function renderLines() {
    const hasFactory = state.selectedFactoryId !== null;
    renderResourceList({
        container: elements.lineList,
        resources: state.lines,
        selectedId: state.selectedLineId,
        emptyTitle: hasFactory
            ? "등록된 생산라인이 없습니다"
            : "공장을 먼저 선택해 주세요",
        emptyDescription: hasFactory
            ? "선택한 공장에 첫 생산라인을 등록해 주세요."
            : "공장을 선택하면 해당 생산라인을 불러옵니다.",
        onSelect: selectLine,
        itemMeta: (line) => line.active ? "운영 중" : "비활성"
    });
}

function renderMachines() {
    const hasLine = state.selectedLineId !== null;
    renderResourceList({
        container: elements.machineList,
        resources: state.machines,
        selectedId: null,
        emptyTitle: hasLine
            ? "등록된 설비가 없습니다"
            : "생산라인을 먼저 선택해 주세요",
        emptyDescription: hasLine
            ? "선택한 라인에 첫 설비를 등록해 주세요."
            : "생산라인을 선택하면 소속 설비를 불러옵니다.",
        itemMeta: (machine) => `설비 #${machine.id}`,
        status: (machine) => machine.status
    });
}

function renderResourceList({
    container,
    resources,
    selectedId,
    emptyTitle,
    emptyDescription,
    onSelect,
    itemMeta,
    status
}) {
    container.replaceChildren();

    if (resources.length === 0) {
        container.append(createEmptyState(emptyTitle, emptyDescription));
        return;
    }

    for (const resource of resources) {
        const item = document.createElement(onSelect ? "button" : "div");
        item.className = "resource-item";
        if (resource.id === selectedId) {
            item.classList.add("is-selected");
        }
        if (onSelect) {
            item.type = "button";
            item.addEventListener("click", () => onSelect(resource.id));
        }

        const top = document.createElement("span");
        top.className = "resource-item-top";

        const code = document.createElement("span");
        code.className = "resource-code";
        code.textContent = resource.code;
        top.append(code);

        if (status) {
            top.append(createStatusChip(status(resource)));
        }

        const name = document.createElement("span");
        name.className = "resource-name";
        name.textContent = resource.name;

        const meta = document.createElement("span");
        meta.className = "resource-meta";
        meta.textContent = itemMeta(resource);

        item.append(top, name, meta);
        container.append(item);
    }
}

function createEmptyState(title, description) {
    const wrapper = document.createElement("div");
    wrapper.className = "empty-state";

    const content = document.createElement("div");
    const heading = document.createElement("strong");
    heading.textContent = title;
    const copy = document.createElement("p");
    copy.textContent = description;

    content.append(heading, copy);
    wrapper.append(content);
    return wrapper;
}

function createStatusChip(status) {
    const labels = {
        AVAILABLE: "AVAILABLE",
        STOPPED: "STOPPED",
        INACTIVE: "INACTIVE"
    };
    const chip = document.createElement("span");
    chip.className = "status-chip";
    if (status === "STOPPED") {
        chip.classList.add("is-stopped");
    }
    if (status === "INACTIVE") {
        chip.classList.add("is-inactive");
    }
    chip.textContent = labels[status] || status;
    return chip;
}

function renderMetrics() {
    elements.factoryCount.textContent = state.factories.length;
    elements.lineCount.textContent = state.lines.length;
    elements.machineCount.textContent = state.machines.length;
    elements.availableCount.textContent = state.machines.filter(
        (machine) => machine.status === "AVAILABLE"
    ).length;
}

function renderDialogContext() {
    const factory = state.factories.find(
        (item) => item.id === state.selectedFactoryId
    );
    const line = state.lines.find(
        (item) => item.id === state.selectedLineId
    );

    elements.openLineDialog.disabled = !factory;
    elements.openMachineDialog.disabled = !line;
    elements.lineFactoryName.textContent = factory?.name || "-";
    elements.machineLineName.textContent = line?.name || "-";
}

function bindDialogs() {
    document.querySelectorAll("[data-open-dialog]").forEach((button) => {
        button.addEventListener("click", () => {
            const dialog = document.querySelector(
                `#${button.dataset.openDialog}`
            );
            if (dialog && !button.disabled) {
                dialog.showModal();
                dialog.querySelector("input")?.focus();
            }
        });
    });

    document.querySelectorAll(".resource-dialog").forEach((dialog) => {
        dialog.querySelectorAll(".dialog-close, .dialog-cancel")
            .forEach((button) => {
                button.addEventListener("click", () => dialog.close());
            });

        dialog.addEventListener("click", (event) => {
            if (event.target === dialog) {
                dialog.close();
            }
        });
    });
}

function bindForms() {
    bindCreateForm("#factory-form", async (form) => {
        const created = await request(API.factories, {
            method: "POST",
            body: JSON.stringify(formValues(form, ["code", "name"]))
        });
        await loadFactories(created.id);
        return "공장이 등록되었습니다.";
    });

    bindCreateForm("#line-form", async (form) => {
        if (state.selectedFactoryId === null) {
            throw new Error("생산라인을 등록할 공장을 먼저 선택해 주세요.");
        }
        const created = await request(API.lines(state.selectedFactoryId), {
            method: "POST",
            body: JSON.stringify(formValues(form, ["code", "name"]))
        });
        const page = await request(
            `${API.lines(state.selectedFactoryId)}?page=0&size=100`
        );
        state.lines = page.content;
        await selectLine(created.id);
        return "생산라인이 등록되었습니다.";
    });

    bindCreateForm("#machine-form", async (form) => {
        if (state.selectedLineId === null) {
            throw new Error("설비를 등록할 생산라인을 먼저 선택해 주세요.");
        }
        await request(API.machines(state.selectedLineId), {
            method: "POST",
            body: JSON.stringify(
                formValues(form, ["code", "name", "status"])
            )
        });
        await selectLine(state.selectedLineId);
        return "설비가 등록되었습니다.";
    });
}

function bindCreateForm(selector, submitAction) {
    const form = document.querySelector(selector);
    form.addEventListener("submit", async (event) => {
        event.preventDefault();
        const submitButton = form.querySelector("[type='submit']");
        submitButton.disabled = true;

        try {
            const successMessage = await submitAction(form);
            form.reset();
            form.closest("dialog").close();
            showToast(successMessage);
        } catch (error) {
            showToast(error.message, true);
        } finally {
            submitButton.disabled = false;
        }
    });
}

function formValues(form, fields) {
    const data = new FormData(form);
    return Object.fromEntries(fields.map((field) => [field, data.get(field)]));
}

function setConnection(connected) {
    elements.connectionStatus.classList.remove(
        "is-checking",
        "is-offline"
    );
    if (!connected) {
        elements.connectionStatus.classList.add("is-offline");
    }
    elements.connectionStatus.lastElementChild.textContent =
        connected ? "서버 연결됨" : "서버 연결 끊김";
}

function showToast(message, isError = false) {
    window.clearTimeout(toastTimer);
    elements.toast.textContent = message;
    elements.toast.classList.toggle("is-error", isError);
    elements.toast.classList.add("is-visible");
    toastTimer = window.setTimeout(() => {
        elements.toast.classList.remove("is-visible");
    }, 3200);
}
