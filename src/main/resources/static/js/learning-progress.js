const STORAGE_KEY = "aps-engine.learning-progress.v1";
const VERSION = 1;

export function loadLearningProgress(storage) {
    try {
        const target = storage || globalThis.localStorage;
        const parsed = JSON.parse(target.getItem(STORAGE_KEY) || "null");
        if (parsed?.version === VERSION && parsed.scenarios) return parsed;
    } catch {
        // 손상되거나 차단된 저장소는 학습과 APS 실행을 막지 않는다.
    }
    return {version: VERSION, scenarios: {}};
}

export function saveLearningProgress(progress, storage) {
    try {
        const target = storage || globalThis.localStorage;
        target.setItem(STORAGE_KEY, JSON.stringify(progress));
    } catch {
        // 브라우저 저장 실패는 핵심 계획 기능과 분리한다.
    }
    return progress;
}

export function markLearningProgress(progress, scenarioKey, status, instanceId) {
    const current = progress.scenarios[scenarioKey] || {};
    const updated = {
        ...progress,
        scenarios: {
            ...progress.scenarios,
            [scenarioKey]: {
                ...current,
                status,
                instanceId: instanceId || current.instanceId || null,
                updatedAt: new Date().toISOString()
            }
        }
    };
    return saveLearningProgress(updated);
}

export async function reconcileLearningProgress(progress, findInstance) {
    let reconciled = progress;
    for (const [key, entry] of Object.entries(progress.scenarios)) {
        if (!entry.instanceId || entry.status === "NEEDS_REVIEW") continue;
        try {
            const instance = await findInstance(entry.instanceId);
            if (!instance || instance.status !== "READY") {
                reconciled = markLearningProgress(
                    reconciled,
                    key,
                    "NEEDS_REVIEW",
                    entry.instanceId
                );
            }
        } catch {
            // 일시적 API 오류로 완료 진도를 지우지 않는다.
        }
    }
    return reconciled;
}
