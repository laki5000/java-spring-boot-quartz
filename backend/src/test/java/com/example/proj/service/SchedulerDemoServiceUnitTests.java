package com.example.proj.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.core.exception.ValidationException;
import com.example.core.scheduler.ISchedule;
import com.example.core.scheduler.ISchedulerService;
import com.example.core.scheduler.ScheduledTask;
import com.example.core.scheduler.SchedulerTaskInput;
import com.example.generated.dto.CreateManualScheduleRequest;
import com.example.generated.dto.GetSchedulesDto;
import com.example.proj.constant.SchedulerConstants;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SchedulerDemoServiceUnitTests {

  private static final String SCHEDULE_ID = "schedule-1";
  private static final String SECOND_SCHEDULE_ID = "schedule-2";
  private static final String THIRD_SCHEDULE_ID = "schedule-3";
  private static final String INPUT_VALUE = "test-input";
  private static final OffsetDateTime EXECUTION_TIME = OffsetDateTime.parse("2030-09-26T14:00:00Z");
  private static final OffsetDateTime START_TIME = OffsetDateTime.parse("2030-09-26T14:00:00Z");
  private static final long INTERVAL_SECONDS = 10L;
  private static final String CRON_EXPRESSION = "0/10 * * * * ?";

  @Mock private ISchedulerService schedulerService;

  private SchedulerDemoService schedulerDemoService;

  @BeforeEach
  void setUp() {
    schedulerDemoService = new SchedulerDemoService(schedulerService);
  }

  @Test
  void testCreateManualSchedule_shouldCreateOnceSchedule_whenTypeIsOnce() {
    // Given
    CreateManualScheduleRequest request =
        new CreateManualScheduleRequest()
            .type(CreateManualScheduleRequest.TypeEnum.ONCE)
            .executionTime(EXECUTION_TIME)
            .input(INPUT_VALUE);

    ArgumentCaptor<ISchedule> scheduleCaptor = ArgumentCaptor.forClass(ISchedule.class);
    ArgumentCaptor<SchedulerTaskInput> inputCaptor =
        ArgumentCaptor.forClass(SchedulerTaskInput.class);

    // When
    String result = schedulerDemoService.createManualSchedule(request);

    // Then
    assertEquals(
        SchedulerConstants.MANUAL_SCHEDULE_ID_PREFIX,
        result.substring(0, SchedulerConstants.MANUAL_SCHEDULE_ID_PREFIX.length()));

    verify(schedulerService)
        .schedule(
            eq(result),
            eq(SchedulerConstants.MANUAL_TASK_ID),
            scheduleCaptor.capture(),
            inputCaptor.capture());

    ISchedule.Once schedule = assertInstanceOf(ISchedule.Once.class, scheduleCaptor.getValue());

    assertEquals(EXECUTION_TIME.toInstant(), schedule.executionTime());
    assertEquals(INPUT_VALUE, inputCaptor.getValue().value());
  }

  @Test
  void testCreateManualSchedule_shouldCreateIntervalSchedule_whenTypeIsInterval() {
    // Given
    CreateManualScheduleRequest request =
        new CreateManualScheduleRequest()
            .type(CreateManualScheduleRequest.TypeEnum.INTERVAL)
            .startTime(START_TIME)
            .intervalSeconds(INTERVAL_SECONDS)
            .input(INPUT_VALUE);

    ArgumentCaptor<ISchedule> scheduleCaptor = ArgumentCaptor.forClass(ISchedule.class);

    // When
    String result = schedulerDemoService.createManualSchedule(request);

    // Then
    verify(schedulerService)
        .schedule(
            eq(result),
            eq(SchedulerConstants.MANUAL_TASK_ID),
            scheduleCaptor.capture(),
            any(SchedulerTaskInput.class));

    ISchedule.Interval schedule =
        assertInstanceOf(ISchedule.Interval.class, scheduleCaptor.getValue());

    assertEquals(START_TIME.toInstant(), schedule.startTime());
    assertEquals(Duration.ofSeconds(INTERVAL_SECONDS), schedule.interval());
  }

  @Test
  void testCreateManualSchedule_shouldCreateCronSchedule_whenTypeIsCron() {
    // Given
    CreateManualScheduleRequest request =
        new CreateManualScheduleRequest()
            .type(CreateManualScheduleRequest.TypeEnum.CRON)
            .expression(CRON_EXPRESSION)
            .input(INPUT_VALUE);

    ArgumentCaptor<ISchedule> scheduleCaptor = ArgumentCaptor.forClass(ISchedule.class);

    // When
    String result = schedulerDemoService.createManualSchedule(request);

    // Then
    verify(schedulerService)
        .schedule(
            eq(result),
            eq(SchedulerConstants.MANUAL_TASK_ID),
            scheduleCaptor.capture(),
            any(SchedulerTaskInput.class));

    ISchedule.Cron schedule = assertInstanceOf(ISchedule.Cron.class, scheduleCaptor.getValue());

    assertEquals(CRON_EXPRESSION, schedule.expression());
  }

  @Test
  void testCreateManualSchedule_shouldThrowValidationException_whenExecutionTimeIsNull() {
    // Given
    CreateManualScheduleRequest request =
        new CreateManualScheduleRequest().type(CreateManualScheduleRequest.TypeEnum.ONCE);

    // When / Then
    assertThrows(
        ValidationException.class, () -> schedulerDemoService.createManualSchedule(request));
  }

  @Test
  void testCreateManualSchedule_shouldThrowValidationException_whenStartTimeIsNull() {
    // Given
    CreateManualScheduleRequest request =
        new CreateManualScheduleRequest()
            .type(CreateManualScheduleRequest.TypeEnum.INTERVAL)
            .intervalSeconds(INTERVAL_SECONDS);

    // When / Then
    assertThrows(
        ValidationException.class, () -> schedulerDemoService.createManualSchedule(request));
  }

  @Test
  void testCreateManualSchedule_shouldThrowValidationException_whenIntervalSecondsIsNull() {
    // Given
    CreateManualScheduleRequest request =
        new CreateManualScheduleRequest()
            .type(CreateManualScheduleRequest.TypeEnum.INTERVAL)
            .startTime(START_TIME);

    // When / Then
    assertThrows(
        ValidationException.class, () -> schedulerDemoService.createManualSchedule(request));
  }

  @Test
  void testCreateManualSchedule_shouldThrowValidationException_whenExpressionIsNull() {
    // Given
    CreateManualScheduleRequest request =
        new CreateManualScheduleRequest().type(CreateManualScheduleRequest.TypeEnum.CRON);

    // When / Then
    assertThrows(
        ValidationException.class, () -> schedulerDemoService.createManualSchedule(request));
  }

  @Test
  void testGetSchedules_shouldReturnMappedSchedules_whenSchedulesExist() {
    // Given
    List<ScheduledTask> scheduledTasks =
        List.of(
            new ScheduledTask(
                SCHEDULE_ID,
                ScheduledTask.ScheduleType.ONCE,
                ScheduledTask.ScheduleStatus.SCHEDULED),
            new ScheduledTask(
                SECOND_SCHEDULE_ID,
                ScheduledTask.ScheduleType.INTERVAL,
                ScheduledTask.ScheduleStatus.PAUSED),
            new ScheduledTask(
                THIRD_SCHEDULE_ID,
                ScheduledTask.ScheduleType.CRON,
                ScheduledTask.ScheduleStatus.COMPLETE));

    when(schedulerService.getSchedules()).thenReturn(scheduledTasks);

    // When
    List<GetSchedulesDto> result = schedulerDemoService.getSchedules();

    // Then
    assertEquals(3, result.size());

    assertEquals(SCHEDULE_ID, result.getFirst().getId());
    assertEquals(GetSchedulesDto.TypeEnum.ONCE, result.getFirst().getType());
    assertEquals(GetSchedulesDto.StatusEnum.SCHEDULED, result.getFirst().getStatus());

    assertEquals(SECOND_SCHEDULE_ID, result.get(1).getId());
    assertEquals(GetSchedulesDto.TypeEnum.INTERVAL, result.get(1).getType());
    assertEquals(GetSchedulesDto.StatusEnum.PAUSED, result.get(1).getStatus());

    assertEquals(THIRD_SCHEDULE_ID, result.get(2).getId());
    assertEquals(GetSchedulesDto.TypeEnum.CRON, result.get(2).getType());
    assertEquals(GetSchedulesDto.StatusEnum.COMPLETE, result.get(2).getStatus());

    verify(schedulerService).getSchedules();
  }

  @Test
  void testGetSchedules_shouldReturnEmptyList_whenNoSchedulesExist() {
    // Given
    when(schedulerService.getSchedules()).thenReturn(List.of());

    // When
    List<GetSchedulesDto> result = schedulerDemoService.getSchedules();

    // Then
    assertEquals(List.of(), result);
    verify(schedulerService).getSchedules();
  }

  @Test
  void testCancelSchedule_shouldCancelSchedule() {
    // When
    schedulerDemoService.cancelSchedule(SCHEDULE_ID);

    // Then
    verify(schedulerService).cancel(SCHEDULE_ID);
  }

  @Test
  void testPauseSchedule_shouldPauseSchedule() {
    // When
    schedulerDemoService.pauseSchedule(SCHEDULE_ID);

    // Then
    verify(schedulerService).pause(SCHEDULE_ID);
  }

  @Test
  void testResumeSchedule_shouldResumeSchedule() {
    // When
    schedulerDemoService.resumeSchedule(SCHEDULE_ID);

    // Then
    verify(schedulerService).resume(SCHEDULE_ID);
  }

  @Test
  void testTriggerSchedule_shouldTriggerSchedule() {
    // When
    schedulerDemoService.triggerSchedule(SCHEDULE_ID);

    // Then
    verify(schedulerService).trigger(SCHEDULE_ID);
  }
}
