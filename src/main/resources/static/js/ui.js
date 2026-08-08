import {state} from "./state.js";

let toastTimer;

export function setOptions(selector, items, label) {
    const select = document.querySelector(selector);
    if (select) fillSelect(select, items, label);
}

export function fillSelect(select, items, label) {
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

export function setConnection(status) {
    const element = document.querySelector("#connection-status");
    element.className = `connection-status ${status === "online" ? "" : `is-${status}`}`.trim();
    element.querySelector("strong").textContent = {
        online: "API ONLINE",
        offline: "API OFFLINE",
        checking: "서버 확인 중"
    }[status];
}

export function groupBy(items, keyFunction) {
    const groups = new Map();
    for (const item of items) {
        const key = keyFunction(item);
        if (!groups.has(key)) groups.set(key, []);
        groups.get(key).push(item);
    }
    return groups;
}

export function values(form, fields) {
    const data = new FormData(form);
    return Object.fromEntries(fields.map((field) => [field, data.get(field)]));
}

export function text(selector, value) {
    document.querySelector(selector).textContent = value;
}

export function formatDateTime(value) {
    if (!value) return "-";
    const parts = offsetDateParts(value, displayOffsetSeconds());
    return `${pad(parts.month)}. ${pad(parts.day)}. ${pad(parts.hour)}:${pad(parts.minute)}`;
}

export function formatTime(value) {
    const parts = offsetDateParts(value, displayOffsetSeconds());
    return `${pad(parts.hour)}:${pad(parts.minute)}`;
}

export function formatAxisTime(value) {
    const parts = offsetDateParts(value, displayOffsetSeconds());
    return `${pad(parts.month)}. ${pad(parts.day)}. ${pad(parts.hour)}:${pad(parts.minute)}`;
}

function displayOffsetSeconds() {
    return state.latestSchedule?.planningOffsetSeconds
        ?? -new Date().getTimezoneOffset() * 60;
}

export function withOffset(value, offsetSeconds) {
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

export function toLocalInput(date) {
    const offset = date.getTimezoneOffset() * 60000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

export function setInputIfEmpty(selector, value) {
    const input = document.querySelector(selector);
    if (input && !input.value) input.value = value;
}

export function number(value) {
    return new Intl.NumberFormat("ko-KR").format(value);
}

export function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

export function showToast(message, isError = false) {
    window.clearTimeout(toastTimer);
    const toast = document.querySelector("#toast");
    toast.textContent = message;
    toast.classList.toggle("is-error", isError);
    toast.classList.add("is-visible");
    toastTimer = window.setTimeout(() => toast.classList.remove("is-visible"), 3600);
}
