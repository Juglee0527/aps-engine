package com.github.juglee0527.apsengine.scheduling;

import java.util.ArrayList;
import java.util.List;

import com.github.juglee0527.apsengine.common.error.ApplicationException;
import com.github.juglee0527.apsengine.common.error.ErrorCode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlannedLeadTimeService {

    private final ScheduleRunRepository scheduleRunRepository;
    private final PlannedLeadTimeCalculator calculator;

    public PlannedLeadTimeService(
            ScheduleRunRepository scheduleRunRepository
    ) {
        this.scheduleRunRepository = scheduleRunRepository;
        this.calculator = new PlannedLeadTimeCalculator();
    }

    @Transactional(readOnly = true)
    public List<PlannedLeadTime> calculate(long scheduleRunId) {
        ScheduleRun scheduleRun = scheduleRunRepository
                .findById(scheduleRunId)
                .orElseThrow(() -> new ApplicationException(
                        ErrorCode.SCHEDULE_RUN_NOT_FOUND
                ));
        List<LeadTimeOperationInput> inputs = new ArrayList<>();
        for (ScheduledOperation scheduledOperation
                : scheduleRun.scheduledOperations()) {
            var order = scheduledOperation.productionOrder();
            var product = order.routing().product();
            inputs.add(new LeadTimeOperationInput(
                    order.id(),
                    order.orderNumber(),
                    product.id(),
                    product.code(),
                    order.releaseAt(),
                    scheduledOperation.endAt(),
                    scheduledOperation.workingMinutes(),
                    scheduledOperation.changeoverMinutes()
            ));
        }
        return calculator.calculate(inputs);
    }
}
