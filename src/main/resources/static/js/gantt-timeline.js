const HOUR = 3_600_000;

export function resolveTimelineWindow(
    schedule,
    tasks,
    filters = {},
    view = {mode: "fit", offset: 0}
) {
    const explicitStart = filters.from
        ? new Date(filters.from).getTime() : null;
    const explicitEnd = filters.to
        ? new Date(filters.to).getTime() : null;
    if (explicitStart != null || explicitEnd != null) {
        const start = explicitStart
            ?? new Date(schedule.planningStart).getTime();
        return {
            start,
            end: Math.max(explicitEnd ?? start + HOUR, start + HOUR)
        };
    }

    const starts = tasks.map((task) => new Date(
        task.changeoverStartAt || task.startAt
    ).getTime());
    const ends = tasks.map((task) => new Date(task.endAt).getTime());
    const firstTask = Math.min(...starts);
    const lastTask = Math.max(...ends);
    if (view.mode === "full") {
        const start = new Date(schedule.planningStart).getTime();
        return {
            start,
            end: Math.max(
                new Date(schedule.schedulingEnd).getTime(),
                start + HOUR
            )
        };
    }
    if (view.mode === "8h" || view.mode === "24h") {
        const windowSize = (view.mode === "8h" ? 8 : 24) * HOUR;
        const base = Math.floor(firstTask / HOUR) * HOUR;
        const start = base + view.offset * windowSize;
        return {start, end: start + windowSize};
    }
    const taskSpan = Math.max(HOUR, lastTask - firstTask);
    const padding = Math.max(HOUR / 2, taskSpan * .06);
    return {start: firstTask - padding, end: lastTask + padding};
}
