package com.example.proj.service;

import com.example.core.logging.LogExecution;
import com.example.core.scheduler.Schedule;
import com.example.core.scheduler.ScheduledTask;
import com.example.core.scheduler.SchedulerPort;
import com.example.core.scheduler.SchedulerTaskInput;
import com.example.core.validation.ValidationUtils;
import com.example.generated.dto.CreateManualScheduleRequest;
import com.example.generated.dto.GetSchedulesDto;
import com.example.proj.constant.SchedulerConstants;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.event.Level;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SchedulerDemoService {

    private final SchedulerPort schedulerPort;

    @LogExecution(level = Level.INFO, logArguments = true, logResult = true)
    public String createManualSchedule(CreateManualScheduleRequest request) {
        String scheduleId = SchedulerConstants.MANUAL_SCHEDULE_ID_PREFIX + UUID.randomUUID();

        Schedule schedule =
                switch (request.getType()) {
                    case ONCE ->
                            Schedule.onceAt(
                                    ValidationUtils.requireNonNull(
                                                    request.getExecutionTime(), "Execution time must not be null")
                                            .toInstant());

                    case INTERVAL ->
                            Schedule.interval(
                                    ValidationUtils.requireNonNull(
                                                    request.getStartTime(), "Start time must not be null")
                                            .toInstant(),
                                    Duration.ofSeconds(
                                            ValidationUtils.requireNonNull(
                                                    request.getIntervalSeconds(), "Interval must not be null")));

                    case CRON ->
                            Schedule.cron(
                                    ValidationUtils.requireNonNull(
                                            request.getExpression(), "Cron expression must not be null"));
                };

        SchedulerTaskInput input = new SchedulerTaskInput(request.getInput());

        schedulerPort.schedule(scheduleId, SchedulerConstants.MANUAL_TASK_ID, schedule, input);

        return scheduleId;
    }

    @LogExecution(level = Level.INFO, logResult = true)
    public List<GetSchedulesDto> getSchedules() {
        return schedulerPort.getSchedules().stream().map(this::toResponse).toList();
    }

    @LogExecution(level = Level.INFO, logArguments = true)
    public void cancelSchedule(String scheduleId) {
        schedulerPort.cancel(scheduleId);
    }

    @LogExecution(level = Level.INFO, logArguments = true)
    public void pauseSchedule(String scheduleId) {
        schedulerPort.pause(scheduleId);
    }

    @LogExecution(level = Level.INFO, logArguments = true)
    public void resumeSchedule(String scheduleId) {
        schedulerPort.resume(scheduleId);
    }

    @LogExecution(level = Level.INFO, logArguments = true)
    public void triggerSchedule(String scheduleId) {
        schedulerPort.trigger(scheduleId);
    }

    private GetSchedulesDto toResponse(ScheduledTask scheduledTask) {
        return new GetSchedulesDto()
                .id(scheduledTask.id())
                .type(GetSchedulesDto.TypeEnum.valueOf(scheduledTask.type().name()))
                .status(GetSchedulesDto.StatusEnum.valueOf(scheduledTask.status().name()));
    }
}