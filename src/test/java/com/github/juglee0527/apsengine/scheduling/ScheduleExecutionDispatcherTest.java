package com.github.juglee0527.apsengine.scheduling;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;

@ExtendWith(MockitoExtension.class)
class ScheduleExecutionDispatcherTest {

    @Mock
    private TaskExecutor taskExecutor;

    @Mock
    private ScheduleExecutionWorker worker;

    @Mock
    private ScheduleExecutionTransactionService transactionService;

    @InjectMocks
    private ScheduleExecutionDispatcher dispatcher;

    @Test
    void marksQueuedExecutionFailedWhenQueueIsFull() {
        doThrow(new TaskRejectedException("queue full"))
                .when(taskExecutor)
                .execute(any(Runnable.class));

        dispatcher.dispatch(31L);

        verify(transactionService).fail(
                31L,
                "스케줄 실행 대기열이 가득 찼습니다."
        );
    }
}
