import {state} from "./state.js";
import {escapeHtml, fillSelect, setOptions} from "./ui.js";

export function renderMasterData() {
    const facilities = document.querySelector("#facility-tree");
    const products = document.querySelector("#product-tree");
    facilities.replaceChildren();
    products.replaceChildren();

    if (state.factories.length === 0) {
        facilities.innerHTML = `<div class="empty-state compact"><strong>생산 자원부터 구성하세요</strong><p>공장 → 라인 → 설비 → 근무시간 순서로 등록합니다.</p></div>`;
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
        products.innerHTML = `<div class="empty-state compact"><strong>제품과 공정을 구성하세요</strong><p>품목을 등록한 뒤 Routing에 공정과 설비를 연결합니다.</p></div>`;
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

export function populateSelects() {
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
