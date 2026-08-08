export const SAMPLE_DATA = {
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

export const SAMPLE_STEP_KEYS = ["resources", "process", "orders", "schedule"];
