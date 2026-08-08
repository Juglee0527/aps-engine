export const API = {
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
    scheduleExecution: (executionId) =>
        `/api/v1/schedules/executions/${executionId}`,
    reschedule: (scheduleRunId) =>
        `/api/v1/schedules/${scheduleRunId}/reschedule`,
    planningDataPreview: "/api/v1/planning-data/imports/preview",
    planningDataImports: "/api/v1/planning-data/imports",
    latestSchedule: "/api/v1/schedules/latest",
    bottlenecks: (scheduleRunId) =>
        `/api/v1/schedules/${scheduleRunId}/bottlenecks`,
    learningScenarios: "/api/v1/learning/scenarios",
    learningScenarioInstances: (scenarioKey) =>
        `/api/v1/learning/scenarios/${scenarioKey}/instances`,
    learningInstanceSchedules: (instanceId) =>
        `/api/v1/learning/instances/${instanceId}/schedules`,
    learningRuleComparison: (instanceId) =>
        `/api/v1/learning/instances/${instanceId}/rule-comparison`,
    learningConstraintImpact: (instanceId) =>
        `/api/v1/learning/instances/${instanceId}/constraint-impact`
};

export async function request(url, options = {}) {
    const {allowNotFound = false, ...fetchOptions} = options;
    const defaultHeaders = fetchOptions.body instanceof FormData
        ? {}
        : {"Content-Type": "application/json"};
    const response = await fetch(url, {
        headers: {...defaultHeaders, ...fetchOptions.headers},
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
