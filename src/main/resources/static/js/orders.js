import {state} from "./state.js";
import {escapeHtml, formatDateTime, number} from "./ui.js";

export function renderOrders(confirmOrder) {
    const summary = document.querySelector("#order-summary-body");
    const full = document.querySelector("#order-table-body");
    summary.replaceChildren();
    full.replaceChildren();
    const pageStatus = document.querySelector("#order-page-status");
    if (pageStatus) {
        pageStatus.textContent = `${number(state.orderPage.totalElements || 0)}건 · ${state.orderPage.page + 1}/${Math.max(1, state.orderPage.totalPages)} 페이지`;
    }
    const previous = document.querySelector("#order-prev-page");
    const next = document.querySelector("#order-next-page");
    if (previous) previous.disabled = state.orderPage.first;
    if (next) next.disabled = state.orderPage.last;
    renderFilterSummary();
    if (state.orders.length === 0) {
        const filtered = Boolean(
            state.orderFilters.query || state.orderFilters.status
        );
        summary.innerHTML = `<tr><td colspan="5"><div class="table-empty">등록된 생산오더가 없습니다.</div></td></tr>`;
        full.innerHTML = `<tr><td colspan="8"><div class="empty-state">
            <strong>${filtered ? "조건에 맞는 생산오더가 없습니다" : "첫 생산오더를 등록해 보세요"}</strong>
            <p>${filtered ? "검색어 또는 상태 조건을 바꾸면 다른 오더를 찾을 수 있습니다." : "품목과 Routing을 준비한 뒤 수량, 납기와 우선순위를 입력합니다."}</p>
            <button class="ghost-button" type="button" data-order-empty-action="${filtered ? "reset" : "master"}">${filtered ? "필터 초기화" : "마스터 데이터 확인"}</button>
        </div></td></tr>`;
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

function renderFilterSummary() {
    const container = document.querySelector("#order-filter-summary");
    const filters = [];
    if (state.orderFilters.query) {
        filters.push(`검색 · ${state.orderFilters.query}`);
    }
    if (state.orderFilters.status) {
        filters.push(`상태 · ${state.orderFilters.status}`);
    }
    container.innerHTML = filters.length === 0
        ? '<span class="filter-summary-empty">전체 생산오더</span>'
        : filters.map((filter) => `<span>${escapeHtml(filter)}</span>`).join("");
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
